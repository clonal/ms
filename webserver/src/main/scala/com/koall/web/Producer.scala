package com.koall.web

import java.io.{File, FileInputStream}
import java.util.Properties

import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.internals.DefaultPartitioner
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.slf4j.LoggerFactory

object Producer {
  final val log = LoggerFactory.getLogger("producer")
  val config = ConfigFactory.parseFile(new File("conf" + File.separator +
    "web" + File.separator + "producer.properties"))
  final val props = new Properties()
  config.entrySet().forEach{ x =>
    props.put(x.getKey, x.getValue.unwrapped().asInstanceOf[String])
  }
  val producer = {
//  def apply(): KafkaProducer[String, String]  = {
    //val brokers = "192.168.1.151:9092,192.168.1.152:9092,192.168.1.153:9092"
    //props.put("metadata.broker.list", brokers)
    //props.put("serializer.class", "kafka.serializer.StringEncoder")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("partitioner.class", classOf[DefaultPartitioner].getName)
    props.put("producer.type", "sync")
    props.put("batch.num.messages", "1")
    props.put("queue.buffering.max.messages", "1000000")
    props.put("queue.enqueue.timeout.ms", "20000000")

    new KafkaProducer[String, String](props)
  }

  def sendMsg(msg: ProducerRecord[String, String]) = {
    log.info(s"send message ${msg.value()}")
    producer.send(msg)
  }

  def getTopic() = props.getProperty("topic")
}