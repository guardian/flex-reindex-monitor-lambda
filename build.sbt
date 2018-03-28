val awsLambdaVersion = "1.2.0"

val awsDeps = Seq(
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.301",
  "com.amazonaws" % "aws-lambda-java-core" % awsLambdaVersion,
  "com.amazonaws" % "aws-lambda-java-events" % awsLambdaVersion,
  "com.gu" %% "scanamo" % "1.0.0-M5"
)

val model = (project in file("./model"))

val lambda = (project in file("./lambda"))
  .settings(libraryDependencies ++= awsDeps)
  .dependsOn(model)

val cli = (project in file("./cli"))
  .settings(libraryDependencies ++= awsDeps)
  .dependsOn(model)

val root = (project in file("."))
  .aggregate(lambda)
  .aggregate(cli)
