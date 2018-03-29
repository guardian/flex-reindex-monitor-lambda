package com.gu.flexiblecontent.reindexmonitor

import scala.collection.JavaConverters._

import java.util.Base64
import org.joda.time.DateTime

import java.nio.ByteBuffer

import com.amazonaws.auth.profile.ProfileCredentialsProvider

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

case class Env(app: String, stack: String, stage: String, dbTable: String) {
  override def toString: String = s"App: $app, Stack: $stack, Stage: $stage, DBTable: $dbTable:\n"
}

object Env {
  def apply(): Env = Env(
    Option(System.getenv("App")).getOrElse("DEV"),
    Option(System.getenv("Stack")).getOrElse("DEV"),
    Option(System.getenv("Stage")).getOrElse("DEV"),
    Option(System.getenv("DynamoTable")).getOrElse("DEV"))
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
    val events = records.map(r => processPayload(r.getData)) collect {
      case Success(ev) =>
        logger.info(s"received event $ev")
        ev
    }
    trackReindex(events)
  }

  def processPayload(payload: ByteBuffer): Try[Event] = ThriftDeserialiser.deserialiseEvent(payload)

  def trackReindex(events: Seq[Event], tracker: Tracker = Tracker.getDefault) = {
    logger.info(s"Recording ${events.length} event(s)")
    val recs = events map { ev =>
      val rec = ReindexEventRecord(ev.contentId, Some(new DateTime().getMillis))
      logger.info(s"Recording event: $rec")
      rec
    }
    tracker.registerReindexEvents(recs)
  }
}

object TestMain {
  def main(args: Array[String]) = {
    val inputString = args.headOption
      .getOrElse("Aii1L/0ASN0BADQDFQIWgPXT7ctYGCQ2YTI1OGVhOS04YTMzLTRjNWYtOWRjZS03MWYxNGRiZThmMjUWpBMAAQCjKIAC")
    println(s"main test ($inputString)")
    val data = Base64.getDecoder().decode(inputString)
    val tracker = new DynamoDBTracker(Some("flex-reindex-monitor-test"), Some(new ProfileCredentialsProvider("composer")))
    Lambda.processPayload(ByteBuffer.wrap(data)) match {
      case Success(ev) => Lambda.trackReindex(Seq(ev), tracker)
      case Failure(err) => println(s"Failed: ${err}")
    }
  }
}
