package com.koall.email

import java.sql.Date

import slick.jdbc.H2Profile.api._

case class Record(id: Long, name: String, email: String, template: String,
                  params: String, state: Int, created: Date)

class RecordTable(tag: Tag) extends Table[Record](tag, "EMAIL_LOG") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def name = column[String]("NAME", O.Default(""))
  def email = column[String]("EMAIL", O.Default(""))
  def template = column[String]("TEMPLATE", O.Default(""))
  def params = column[String]("PARAMS", O.Default(""))
  def state = column[Int]("STATE", O.Default(1))
  def created = column[Date]("CREATED", O.Default(new Date(System.currentTimeMillis())))
  def * = (id, name, email, template, params, state, created) <> (Record.tupled, Record.unapply)
}


