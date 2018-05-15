package com.koall.email

import java.io.{File, FileInputStream}
import java.util.Properties

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import spray.json.{JsString, _}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.concurrent.forkjoin.ForkJoinPool


class Collector extends Actor with ActorLogging {
  @volatile var mailCount : Int = 0
  // 创建一个Cluster实例
  val cluster = Cluster(context.system)
  // 用来缓存下游注册过来的子系统ActorRef
  var workers = IndexedSeq.empty[ActorRef]
  // 消费者actor
  val consumer = context.actorOf(ConsumerActor.props, "consumer")

  final val logger = LoggerFactory.getLogger("collector")

  override def receive: Receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
    case UnreachableMember(member) =>
      log.info("Member detected as Unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
    case _: MemberEvent => // ignore
    case msg: StartConsume =>
      logger.info(s"${context.self} 开始读取kafka队列 on ${msg.topics}")
      consumer ! msg
    case Registration =>
      // watch发送注册消息的interceptor，如果对应的Actor终止了，会发送一个Terminated消息
      context watch sender
      workers = workers :+ sender
      log.info("com.koall.email.Processor registered: " + sender)
      log.info("Registered Processors: " + workers.size)
    case Terminated(actorRef) =>
      workers = workers.filterNot(_ == actorRef)
    case job: MailJob =>
      log.info("Raw message: value=" + job.json)
      val json = job.json.parseJson.asJsObject.fields map {
        case (k, v) => (k ,v.asInstanceOf[JsString].value)
      }
      mailCount += 1
      if(workers.nonEmpty && json.nonEmpty) {
        dispatch(EMailTask(job.tp, json))
      }
//      loop(job.tp, json)
    case _ => log.info("receive unknown message!")
  }

  def dispatch(task: EMailTask) = {
    val index = (if (mailCount < 0) 0 else mailCount) % workers.size
    workers(index) ! task
    log.info("Details: index=" + index + ", workers=" + workers.size)
  }

  def loop(tp: String, json: Map[String,String]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    if (mailCount < 100) {
      if(workers.nonEmpty && json.nonEmpty) {
        mailCount += 1
        dispatch(EMailTask(tp, json))
      }
      context.system.scheduler.scheduleOnce(10 millis, new Runnable {
        override def run(): Unit = loop(tp, json)
      })
    }
  }
}

object Collector {
  def props = Props(new Collector)

  def main(args: Array[String]): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool())
    val conf = ConfigFactory.parseFile(new File("conf" + File.separator +
      "collect" + File.separator + "nodes.properties"))
    //    val conf = ConfigFactory.load("webserver.properties")
    val ports = conf.getString("ports").split(",")
    ports.foreach { port =>
      // 创建一个Config对象
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
        .withFallback(ConfigFactory.parseString("akka.cluster.roles = [collector]"))
        .withFallback(ConfigFactory.parseFile(new File("conf" + File.separator +
          "collect" + File.separator + "application.conf")))
      // 创建一个ActorSystem实例
      val system = ActorSystem("email-cluster-system", config)
      val actor = system.actorOf(Props[Collector], name = "collectingActor")
      actor ! StartConsume(ConsumerActor.getTopics)
    }
  }
}