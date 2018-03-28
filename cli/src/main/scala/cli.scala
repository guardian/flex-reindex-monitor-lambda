package com.gu.flexiblecontent.reindexmonitor

import com.gu.scanamo.{ Scanamo, Table }

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder

sealed trait Command
object Command {
  case object count extends Command
}

case class Config(
  command: Command = Command.count,
  databaseTable: Option[String] = Some(Config.dbEnvVar),
  awsProfile: Option[String] = None
)

object Config {
  val dbEnvVar = "FLEX_REINDEX_DB"
}

class FlexReindexMonitorCli(tableName: String, awsProfile: Option[String] = None) {
  lazy val dbClient = {
    val bld = AmazonDynamoDBClientBuilder.standard()
    awsProfile.foreach(c => bld.setCredentials(new ProfileCredentialsProvider(c)))
    bld.build()
  }

  lazy val table = Table[ReindexEventRecord](tableName)

  def count: Int = Scanamo.exec(dbClient)(for(items <- table.scan()) yield items.length)

  def doCommand(c: Command) = c match {
    case Command.count => println(s"Count: $count")
  }
}

object FlexReindexMonitorCli {

  val progVersion = "0.0.0"

  val optParser = new scopt.OptionParser[Config]("flex-reindex-monitor") {
    head("Reindex Monitor", progVersion)

    opt[String]("profile")
      .text("which AWS credentials profile to use")
      .action((prof, conf) => conf.copy(awsProfile = Some(prof)))

    arg[String]("tableName")
      .optional
      .text(s"The DynamoDB table name to connect to (if missing uses env var ${Config.dbEnvVar})")
      .action((tbl, conf) => conf.copy(databaseTable = Some(tbl)))

    cmd("count").action(_.copy(command = Command.count))
  }

  def main(args: Array[String]): Unit = {
    optParser.parse(args, Config()) foreach { config =>
      println(config)
      val cli = new FlexReindexMonitorCli(config)
    }
  }

}
