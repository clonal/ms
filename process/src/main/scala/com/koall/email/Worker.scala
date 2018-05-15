package com.koall.email

import java.io.StringWriter
import java.sql.Date
import java.time.LocalDateTime

import akka.actor.{Actor, ActorLogging, ActorRef}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import spray.json._

class Worker extends Actor with ActorLogging{
  final val SEND_SUCCESS = 1
  final val SEND_FAIL = 0

  final val logger = LoggerFactory.getLogger("worker")

  override def receive: Receive = {
    case task: EMailTask => doWork(sender, task)
    case FailedSend(num, task) => doWork(sender, task, num)
    case _ =>
  }

  def doWork(sender: ActorRef, task: EMailTask, retry: Int = 0) = {
    send(task).andThen{
      case Success(_) =>
        store(task, SEND_SUCCESS).onComplete{
          case Success(_) =>
//            println(s"工作完毕! ")
//            logger.info(s"工作完毕! ")
            sender ! CompletedTask(task)
          case Failure(t) =>
//            println(s"工作失败! ")
//            logger.info(s"工作失败! ")
            sender ! FailedStore(task)
        }
      case Failure(_) =>
        logger.info(s"开始重试 ${task.method} -> ${task.json} on ${LocalDateTime.now}")
        sender ! FailedSend(retry, task)
    }
  }

  def send(mail: EMailTask): Future[_] = {
    val method = mail.method
    logger.info(s"${context.self} 开始工作 ${mail.method} -> ${mail.json} on ${LocalDateTime.now}")
    val params = mail.json
    params.get("to") match {
      case Some(to) =>
        MailConfig.getTemplate(method).map { template =>
          val html = FreemarkerUtil.html(template, params)(new StringWriter())
          MailUtil.sendHtmlMail(to, params.getOrElse("cc", ""),
            MailConfig.getTitle(method), html)
        } match {
          case Some(f) =>
//            println(s"发送完毕! ")
//            logger.info(s"发送完毕! ")
            f
          case _ =>
//            println(s"发送失败1! ")
//            logger.info(s"发送失败1! ")
            log.info("wrong method")
            Future.failed(new RuntimeException("wrong method"))
        }
      case None =>
        log.info("send to nobody")
        Future.failed(new RuntimeException("send to nobody"))
    }
  }

  def store(task: EMailTask, success: Int) = {
    logger.info(s"${context.self} 开始存储 ${task.method} -> ${task.json} on ${LocalDateTime.now}")
    val params = task.json
    val record = Record(0, params.getOrElse("name", ""),
      params.getOrElse("to", ""), task.method, params.toJson(StringMapJsonFormat).compactPrint,
      success, new Date(System.currentTimeMillis()))
    DBUtil.store(record)
  }

  implicit object StringMapJsonFormat extends RootJsonFormat[collection.immutable.Map[String, String]] {
    def write(c: collection.immutable.Map[String, String]) = {
      c.foldLeft(JsObject())((obj, entry) => JsObject(obj.fields + (entry._1 -> JsString(entry._2))))
    }
    def read(value: JsValue) = {
      value.asJsObject.fields map {
        case (k, js) => (k, js.asInstanceOf[JsString].value)
      }
    }
  }
}

case class CompletedTask(task: EMailTask) extends Serializable
case class FailedStore(task: EMailTask) extends Serializable
case class FailedSend(num: Int, task: EMailTask) extends Serializable
case class FailedTask(task: EMailTask) extends Serializable