package com.koall.email

import java.io.File
import java.sql.Date

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import slick.jdbc.OracleProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn
import scala.util.{Failure, Success}

object DBUtil {
  val conf = ConfigFactory.parseFile(new File("conf" + File.separator +
    "process" + File.separator + "application.conf"))
  val db = Database.forConfig("db", conf)
  val records = TableQuery[RecordTable]
  final val log = LoggerFactory.getLogger("dbutil")

  def store(record: Record) = {
    db.run(records += record).andThen{
      case Success(_) =>
//        println(s"插入成功! ")
        log.info(s"insert record ${record.template},${record.params} success")
      case Failure(t) =>
//        println(s"插入失败! ${t.getMessage}")
        log.info(s"insert record ${record.template},${record.params} fail: ${t.getMessage}")
    }
  }

//  def main(args: Array[String]): Unit = {
//    try {
//      val createAction =
//        sqlu"""create table EMAIL_LOG
//              (
//                id       number(10) generated always as identity,
//                name     nvarchar2(255),
//                email    nvarchar2(255),
//                template nvarchar2(255),
//                params   nvarchar2(255),
//                state    number(1),
//                created  date
//              )
//              """
//      db.run(createAction).andThen {
//        case Success(_) =>
//          println("insert success")
//        case Failure(t) =>
//          println(s"insert fail: ${t.getMessage}")
//      }
//      val insertAction = records += Record(0, "d", "ss", "dd", "dd", 1, new Date(System.currentTimeMillis()))
//
//      val userId = (records returning records.map(_.id)) += Record(0, "d", "ss", "dd", "dd", 1, new Date(System.currentTimeMillis()))
//      db.run(userId).onComplete{
////      db.run(insertAction).onComplete{
//        case Success(s) =>
//          println(s"s: $s")
//        case Failure(t) =>
//          println(s"t: ${t.getMessage}")
//      }
//    } catch {
//      case e: Exception => println(s"error: ${e.getStackTrace}")
//    }
//    StdIn.readLine()
//  }

}