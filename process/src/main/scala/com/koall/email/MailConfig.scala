package com.koall.email

import java.io.File

import com.typesafe.config.ConfigFactory
import spray.json.{JsArray, JsBoolean, JsNull, JsNumber, JsObject, JsString, JsValue, _}

import scala.io.Source

case class MailConfig(method: String, title: String, template: String)

object MailConfig {

  var mailConfig: Map[String, MailConfig] = Map.empty

  def init() = {
    val path = new File("conf" + File.separator +
      "process" + File.separator + "mail.json").toPath
//    val path = new File(getClass.getResource("/" + "mail.json").getPath)
    mailConfig = loadConfig(path.toString)
  }

  def loadConfig(path: String) = {
    val source = Source.fromFile(path)
    val str = source.mkString
    val json = str.parseJson
    val m = transform(json).asInstanceOf[Map[String, Any]]
    source.close
    m.map{
      case (k, v) =>
        val info = v.asInstanceOf[Map[String, Any]]
        k -> MailConfig(k, info("title").asInstanceOf[String], info("template").asInstanceOf[String])
    }
  }

  def transform(j: JsValue): Any = {
    def rec(v: JsValue): Any = (v: @unchecked) match {
      case JsObject(l) => l.foldLeft(Map[String, Any]()) {
          //          case (m, (name, _: JsUndefined)) => m
          case (m, (name, JsNull)) => m + (name -> null)
          case (m, (name, value)) => m + (name -> rec(value))
        }
      case JsArray(l) => l.map(rec).toList
      case JsString(value) => value
      case JsNumber(value) =>
        val doubleValue = value.doubleValue
        if (scala.math.abs(doubleValue - doubleValue.round) < 1e-7) {
          val longValue = doubleValue.round
          if (longValue != longValue.toInt)
            longValue
          else
            longValue.toInt
        }
        else
          doubleValue
      case JsBoolean(value) => value
      case JsNull => null
    }
    rec(j)
  }

  def getTemplate(method: String) = {
    mailConfig.get(method).map(_.template)
  }

  def getTitle(method: String) = {
    mailConfig.get(method).map(_.title).getOrElse("From BitArk")
  }
}
