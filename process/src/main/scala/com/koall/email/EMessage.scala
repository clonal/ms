package com.koall.email

trait EMessage extends Serializable

case class StartConsume(topics: Seq[String]) extends EMessage

case class MailJob(tp: String, json: String) extends EMessage

case class Store() extends EMessage

case class Failed() extends EMessage

case object Registration extends Serializable

case class EMailTask(method: String, json: Map[String, String]) extends Serializable

//abstract class EMailTask(json: String) extends Serializable {
//  def template: String
//  def parameterlize: Map[String, String]
//}

//case class ActivationTask(to: String, name: String, link: String) extends EMailTask {
//  val template = "activation.html"
//  override def parameterlize: Map[String, String] = Map("to" -> to, "name" -> name, "link" -> link)
//  def this(json: Map[String, String]) = this(json.getOrElse("to", ""),
//    json.getOrElse("name", ""), json.getOrElse("link", ""))
//}
//
//case class ResetPwdTask(to: String, name: String, link: String) extends EMailTask {
//  val template = "resetpwd.html"
//  override def parameterlize: Map[String, String] = Map("to" -> to, "name" -> name, "link" -> link)
//  def this(json: Map[String, String]) = this(json.getOrElse("to", ""),
//    json.getOrElse("name", ""), json.getOrElse("link", ""))
//}
//
//case class AuthorizationTask(to: String, name: String, link: String) extends EMailTask {
//  val template = "authorization.html"
//  override def parameterlize: Map[String, String] = Map("to" -> to, "name" -> name, "link" -> link)
//  def this(json: Map[String, String]) = this(json.getOrElse("to", ""),
//    json.getOrElse("name", ""), json.getOrElse("link", ""))
//}

//object EMailTask {
//  def apply(tp: String, json: Map[String, String]): EMailTask = tp match {
//    case "activation" => new ActivationTask(json)
//    case "resetpwd" => new ResetPwdTask(json)
//    case "authorization" => new AuthorizationTask(json)
//  }
//}