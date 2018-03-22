package com.gu.flexiblecontent.reindexmonitor

import scala.collection.JavaConverters._

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.transport.TIOStreamTransport
import scala.util.{ Try, Success, Failure }

import com.amazonaws.services.lambda.runtime.{ Context, RequestHandler }
import org.slf4j.{ Logger, LoggerFactory }
import com.amazonaws.services.lambda.runtime.events.KinesisEvent
import com.gu.flexiblecontent.model.thrift.Event

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

object ThriftDeserialiser {
  def deserialiseEvent(input: ByteBuffer): Try[Event] = {
    Try {
      val bis = new ByteArrayInputStream(input.array());
      val transport = new TIOStreamTransport(bis)
      val protocol = new TCompactProtocol(transport)
      Event.decode(protocol)
    }
  }
}

object Lambda extends RequestHandler[KinesisEvent, Unit] {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def handleRequest(input: KinesisEvent, context: Context): Unit = {
    val env = Env()
    logger.info(s"Starting $env")
    process(input, env)
  }

  def process(data: KinesisEvent, env: Env): Unit = {
    val records = data.getRecords().asScala.map(_.getKinesis())
    for (rec <- records) {
      ThriftDeserialiser.deserialiseEvent(rec.getData()) match {
        case Success(ev) => logger.info(s"decoded event: $ev")
        case Failure(err) => logger.info(s"failed: $err")
      }
    }
  }
}

object TestMain {
  def main(args: Array[String]) = {
    println("main test")
  }
}
