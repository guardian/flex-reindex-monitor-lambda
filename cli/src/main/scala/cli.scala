package com.gu.flexiblecontent.reindexmonitor

import scala.collection.JavaConverters._

import scala.util.{ Try, Success, Failure }

import com.gu.scanamo.{ Scanamo, Table }

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder

class InvalidConfig(msg: String) extends Exception(msg)

abstract class Command(name: String) {
  def action(conf: Config): Try[Unit]
  def run(conf: Config): Try[Unit] = {
    if(conf.verbose) println(s"Running command: $name")
    action(conf)
  }
}

object Command {
  case object count extends Command("count") {
    def action(conf: Config) = Try {
      println(Scanamo.exec(conf.buildClient)(for (items <- conf.getTable.scan()) yield items.length))
    }
  }
  case object findTable extends Command("findTable") {
    def action(conf: Config) = Try {
      println(s"Add one of these tables in the env var ${Config.dbEnvVar} or pass on the command line")
      val res = conf.buildClient.listTables(Config.dbPrefix).getTableNames.asScala.filter(_.startsWith(Config.dbPrefix))
      println(res.map("\t- " + _).mkString("\n"))
    }
  }
}

case class Config(
  command: Command = Command.count,
  databaseTable: Option[String] = Option(System.getenv(Config.dbEnvVar)),
  awsProfile: Option[String] = Some("composer"),
  region: String = "eu-west-1",
  verbose: Boolean = true) {
  def buildClient = {
    val bld = AmazonDynamoDBClientBuilder.standard()
    awsProfile.foreach(c => bld.setCredentials(new ProfileCredentialsProvider(c)))
    bld.build()
  }
  def getTable = databaseTable match {
    case Some(name) => Table[ReindexEventRecord](name)
    case None => throw new InvalidConfig("missing tablename")
  }
}

object Config {
  val dbEnvVar = "FLEX_REINDEX_DB"
  val dbPrefix = "flex-reindex-monitor-lambda"
}

class FlexReindexMonitorCli(tableName: String, awsProfile: Option[String] = None) {

}

object FlexReindexMonitorCli {

  val progVersion = "0.0.0"

  val optParser = new scopt.OptionParser[Config]("flex-reindex-monitor") {
    head("Reindex Monitor", progVersion)

    help("help").text("help text")

    opt[String]("profile")
      .text("which AWS credentials profile to use")
      .action((prof, conf) => conf.copy(awsProfile = Some(prof)))

    opt[String]("table")
      .optional
      .text(s"The DynamoDB table name to connect to (if missing uses env var ${Config.dbEnvVar})")
      .action((tbl, conf) => conf.copy(databaseTable = Some(tbl)))

    cmd("count")
      .text("Count the number of seen records")
      .action((_, c) => c.copy(command = Command.count))
    cmd("findTable")
      .text("list possible candidates for the table name")
      .action((_, c) => c.copy(command = Command.findTable))
  }

  def main(args: Array[String]): Unit = {
    optParser.parse(args, Config()) foreach { config =>
      config.command.run(config) match {
        case Failure(err) =>
          if(config.verbose)
            err.printStackTrace()
          else
            println(s"Error: $err")
        case Success(_) =>
      }
    }
  }
}
