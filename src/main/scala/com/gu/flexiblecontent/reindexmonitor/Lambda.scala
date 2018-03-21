package com.gu.flexiblecontent.reindexmonitor

import scala.collection.JavaConverters._

import com.amazonaws.services.lambda.runtime.{ Context, RequestHandler }
import org.slf4j.{ Logger, LoggerFactory }
import com.amazonaws.services.lambda.runtime.events.KinesisEvent

/**
 * This is compatible with aws' lambda JSON to POJO conversion.
 * You can test your lambda by sending it the following payload:
 * {"name": "Bob"}
 */

case class Env(app: String, stack: String, stage: String) {
  override def toString: String = s"App: $app, Stack: $stack, Stage: $stage\n"
}

object Env {
  def apply(): Env = Env(
    Option(System.getenv("App")).getOrElse("DEV"),
    Option(System.getenv("Stack")).getOrElse("DEV"),
    Option(System.getenv("Stage")).getOrElse("DEV"))
}

object Lambda extends RequestHandler[KinesisEvent, Unit] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /*
   * This is your lambda entry point
   */
  def handleRequest(input: KinesisEvent, context: Context): Unit = {
    val env = Env()
    logger.info(s"Starting $env")
    process(input, env)
  }

  /*
   * I recommend to put your logic outside of the handler
   */
  def process(data: KinesisEvent, env: Env): Unit = {
    val records = data.getRecords().asScala
    logger.info(s"Recieved: ${records.length} record(s)")
  }
}
