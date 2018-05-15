package com.koall.web

import java.io.{File, FileInputStream}
import java.time.LocalDateTime
import java.util.Properties

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import com.koall.web.Producer.getClass
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

import scala.io.StdIn

object WebServer {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  final val log = LoggerFactory.getLogger("webserver")

  def main(args: Array[String]) {
    val conf = ConfigFactory.parseFile(new File("conf" + File.separator +
      "web" + File.separator + "webserver.properties"))
//    val conf = ConfigFactory.load("webserver.properties")
    val host = conf.getString("host")
    val port = conf.getString("port")
    //TODO shutdown message
    val services: Flow[HttpRequest, HttpResponse, Any] = path("sendMail" / """\w+""".r) { key =>
      post {
        entity(as[String]) { json =>
          Producer.sendMsg(new ProducerRecord[String, String](Producer.getTopic(),
            key, json))
          complete(HttpEntity.Empty)
        }
      }
    }

    val bindingFuture = Http().bindAndHandle(services, host, port.toInt)
    log.info(s"Server online at http://$host:$port on ${LocalDateTime.now}")
    println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done
  }
}