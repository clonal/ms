package com.koall.email

import java.io.File
import java.nio.charset.StandardCharsets

import com.typesafe.config.ConfigFactory
import courier.{Envelope, Mailer, Multipart, Text}
import javax.mail.internet.{InternetAddress, MimeBodyPart}

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}

object MailUtil {

  implicit def str2addr(str: String): InternetAddress = {
    new InternetAddress(str)
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  val conf = ConfigFactory.parseFile(new File("conf" + File.separator +
    "process" + File.separator + "application.conf"))

  val mailer: Mailer = {
    val host = conf.getString("smtp.host")
    val port = conf.getInt("smtp.port")
    val name = conf.getString("smtp.username")
    val pwd = conf.getString("smtp.password")

    Mailer(host, port)
      .auth(true)
      .as(name, pwd)
      .startTtls(true).sslSocketFactory()
  }

  val from = conf.getString("smtp.from")

  def sendTxtMail(to: String, cc: String, subject: String, txt: String): Future[Unit] = {
    mailer(Envelope.from(from)
      .to(to)
      .cc(cc)
      .subject(subject)
      .content(Text(txt)))
//      .onComplete {
//      case Success(_) => println("send success")
//      case Failure(t) => println(s"error occured: ${t.getMessage}")
//    }
  }

  def sendHtmlMail(to: String, cc: String, subject: String, html: String): Future[Unit] = {
    val charset = StandardCharsets.UTF_8
    mailer(Envelope.from(from)
      .to(to)
      .subject(subject, charset)
      .content(Multipart().add({
        val part = new MimeBodyPart
        part.setText(html, charset.name(), "html")
        part
      }
      )))
  }
}
