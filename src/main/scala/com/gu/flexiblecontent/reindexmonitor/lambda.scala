package com.gu.flexiblecontent.reindexmonitor

import scala.collection.JavaConverters._

import java.util.Base64

import java.nio.ByteBuffer

import com.amazonaws.services.lambda.runtime.{ Context, RequestHandler }
import org.slf4j.{ Logger, LoggerFactory }
import com.amazonaws.services.lambda.runtime.events.KinesisEvent
import com.gu.flexiblecontent.model.thrift.Event
import scala.util.{ Try, Success, Failure }

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

  def handleRequest(input: KinesisEvent, context: Context): Unit = {
    val env = Env()
    logger.info(s"Starting $env")
    processKinesisEvent(input, env)
  }

  def processKinesisEvent(data: KinesisEvent, env: Env): Unit = {
    val records = data.getRecords().asScala.map(_.getKinesis())
    records foreach { rec => processPayload(rec.getData()) }
  }

  def processPayload(payload: ByteBuffer): Unit = {
    ThriftDeserialiser.deserialiseEvent(payload) match {
      case Success(ev) => logger.info(s"decoded event: $ev")
      case Failure(err) => logger.info(s"failed: $err")
    }
  }
}

object TestMain {
  def main(args: Array[String]) = {
    val inputString = args.headOption
      .getOrElse("Aii1L/0ASN0BADQDFQIWgPXT7ctYGCQ2YTI1OGVhOS04YTMzLTRjNWYtOWRjZS03MWYxNGRiZThmMjUWpBMAAQCjKIAC")
    println(s"main test ($inputString)")
    val data = Base64.getDecoder().decode(inputString)
    Lambda.processPayload(ByteBuffer.wrap(data))
  }
}
