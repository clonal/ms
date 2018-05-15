package com.koall.email

import java.io._
import java.nio.file.Paths

import freemarker.template.{Configuration, TemplateExceptionHandler}

import scala.collection.JavaConverters._

object FreemarkerUtil {

  val dir = new File("conf" + File.separator +
    "template")

  val cfg = new Configuration(Configuration.VERSION_2_3_22)
  cfg.setDirectoryForTemplateLoading(dir)
  cfg.setDefaultEncoding("UTF-8")
  cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)

  def html(tmpName: String, params: Map[String, String]): StringWriter => String = { out =>
    val template = cfg.getTemplate(tmpName, "UTF-8")

    template.process(params.asJava, out)
    out.flush()
    out.close()
    out.getBuffer.toString
  }
}

case class Sample(name: String, website: String ,link: String)