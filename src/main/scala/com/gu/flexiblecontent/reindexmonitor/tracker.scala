package com.gu.flexiblecontent.reindexmonitor

import com.gu.scanamo.{ Scanamo, Table }
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.auth.AWSCredentialsProvider

case class ReindexEventRecord(composerId: String, seenAt: Long)

trait Tracker {
  def registerReindexEvent(event: ReindexEventRecord): Unit
}

object Tracker {
  lazy val getDefault = new DynamoDBTracker()
}

class DynamoDBTracker(defaultTableName: Option[String] = None, creds: Option[AWSCredentialsProvider] = None) extends Tracker {
  val client = {
    val bld = AmazonDynamoDBClientBuilder.standard()
    creds foreach { c => println(s"adding creds: $c"); bld.setCredentials(c) }
    bld.build()
  }

  val tableName = defaultTableName getOrElse {
    val env = Env()
    env.dbTable
  }
  val table = Table[ReindexEventRecord](tableName)

  def registerReindexEvent(event: ReindexEventRecord) = {
    val op = table.put(event)
    Scanamo.exec(client)(op)
  }

}
