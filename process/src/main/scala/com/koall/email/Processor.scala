package com.koall.email

import java.io.File
import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, ActorPath, ActorRef, ActorSystem, PoisonPill, Props, RootActorPath}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, Member, MemberStatus}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.concurrent.forkjoin.ForkJoinPool

class Processor extends Actor with ActorLogging {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool())
  // 创建一个Cluster实例
  val cluster = Cluster(context.system)
  // 用来缓存下游注册过来的子系统ActorRef

  final val MAX_RETRY = 10
  final val logger = LoggerFactory.getLogger(Processor.getClass)

  override def preStart(): Unit = {
    // 订阅集群事件
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberUp], classOf[UnreachableMember], classOf[MemberEvent])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
      // 将processor注册到上游的collector中
      register(member, getCollectorPath)
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up).foreach(register(_, getCollectorPath))
    case UnreachableMember(member) =>
      log.info("Member detected as Unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
    case _: MemberEvent => // ignore
    case task: EMailTask => process(task)
    case t: CompletedTask =>
      logger.info(s"发送成功 : ${t.task.method} -> ${t.task.json} on ${LocalDateTime.now}")
      sender ! PoisonPill
    case f: FailedStore =>
      logger.info(s"存储失败: ${f.task.method} -> ${f.task.json} on ${LocalDateTime.now}")
      sender ! PoisonPill
    case f: FailedSend =>
      if (f.num > MAX_RETRY) {
        logger.info(s"发送失败: ${f.task.method} -> ${f.task.json} on ${LocalDateTime.now}")
        sender ! PoisonPill
      } else {
        sender ! FailedSend(f.num + 1, f.task, f.msg)
      }
    case _ => log.info("receive unknown message!")
  }

  /**
    * 下游子系统节点发送注册消息
    */
  def register(member: Member, createPath: Member => ActorPath): Unit = {
    val actorPath = createPath(member)
    log.info("Actor path: " + actorPath)
    val actorSelection = context.actorSelection(actorPath)
    actorSelection ! Registration
  }

  def getCollectorPath(member: Member): ActorPath = {
    RootActorPath(member.address) / "user" / "collectingActor"
  }

  def process(task: EMailTask) = {
    implicit val timeout = Timeout(10 seconds)
    val worker = context.actorOf(Props[Worker])
    logger.info(s"开始任务 ${task.method}: ${task.json}")
//    println(s"开始任务 ${task.method}: ${task.json}")
    worker ! task
  }

}

object Processor {
  def props = Props(new Processor)

  def main(args: Array[String]): Unit = {
    MailConfig.init()
    val config = ConfigFactory.parseFile(new File("conf" + File.separator +
      "process" + File.separator + "nodes.properties"))
    val ports = config.getString("ports").split(",")
    // 启动了5个EventProcessor
    ports foreach { port =>
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
        .withFallback(ConfigFactory.parseString("akka.cluster.roles = [processor]"))
        .withFallback(ConfigFactory.parseFile(new File("conf" + File.separator +
          "process" + File.separator + "application.conf")))
      val system = ActorSystem("email-cluster-system-pro", config)
      val processingActor = system.actorOf(Props[Processor], name = "processingActor")
      system.log.info("Processing Actor: " + processingActor)
    }
  }
}
