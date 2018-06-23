package com.koall.email

import java.io.{File, FileInputStream}
import java.time.LocalDateTime
import java.util.Properties

import akka.actor.{Actor, ActorLogging, Props}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory


class ConsumerActor extends Actor with ActorLogging {

  val consumer: KafkaConsumer[String, String] = init()
  final val logger = LoggerFactory.getLogger(("consumer"))

  override def receive: Receive = {
    case msg: StartConsume =>
      consume(msg.topics)
    case _ =>
  }

  def init(): KafkaConsumer[String, String] = {
    val props = ConsumerActor.prop

    props.put("group.id", "mailTopic")
    props.put("key.deserializer",
      "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer",
      "org.apache.kafka.common.serialization.StringDeserializer")
    new KafkaConsumer[String, String](props)
  }

  def consume(seq: Seq[String]) = {
    import scala.collection.JavaConversions._
    consumer.subscribe(seq)
    while (true) {
      val records = consumer.poll(100)
      for (record <- records) {
        context.parent ! MailJob(record.key, record.value)
        logger.info(s"将kafka队列消息转化为MailJob:  ${record.key} -> ${record.value} on ${LocalDateTime.now}!")
      }
    }
  }
}

object ConsumerActor {
  val prop = getProp

  def props = Props(new ConsumerActor)

  def getProp = {
    val props = new Properties()
    val config = ConfigFactory.parseFile(new File("conf" + File.separator +
      "collect" + File.separator + "consumer.properties"))
    //    val conf = ConfigFactory.load("webserver.properties")
    config.entrySet().forEach{ x =>
      props.put(x.getKey, x.getValue.unwrapped().asInstanceOf[String])
    }
    props
  }

  def getTopics = {
    prop.getProperty("topics").split(",")
  }
}
