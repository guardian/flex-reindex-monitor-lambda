name := "flex-reindex-monitor-lambda"

organization := "com.gu"

description:= "Monitor a reindex operation by observing a kinesis stream."

version := "1.0"

scalaVersion := "2.12.4"

resolvers += Resolver.bintrayRepo("guardian", "editorial-tools")

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

val awsLambdaVersion = "1.2.0"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % awsLambdaVersion,
  "com.amazonaws" % "aws-lambda-java-events" % awsLambdaVersion,
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "com.gu" %% "flexible-model" % "0.0.13",
)

enablePlugins(RiffRaffArtifact)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"${name.value}-cfn/cfn.yaml")
