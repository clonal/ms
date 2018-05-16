name := "ko-mail"

version := "0.1"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.12"

resolvers += "lightshed-maven" at "http://dl.bintray.com/content/lightshed/maven"

val commonDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion % Test,
  "org.apache.kafka" %% "kafka" % "1.1.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

val httpDependencies = commonDependencies ++ Seq(
  "com.typesafe.akka" %% "akka-http"   % "10.1.1",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion
)

val clusterDependencies = Seq(
  "com.typesafe.akka" %% "akka-remote"        % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster"       % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-contrib"       % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1",
)

val processDependencies = Seq(
  "com.typesafe.slick" %% "slick" % "3.2.3",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.3",
  "ch.lightshed" %% "courier" % "0.1.4",
  "org.freemarker" % "freemarker" % "2.3.28"
)

lazy val webserver = (project in file("webserver")).
  settings(
    libraryDependencies ++= commonDependencies ++ httpDependencies
  )

lazy val collect = (project in file("collect")).
  settings(
    libraryDependencies ++= commonDependencies ++ clusterDependencies
  )

lazy val process = (project in file("process")).
  settings(
    libraryDependencies ++= commonDependencies ++ clusterDependencies
      ++ processDependencies ++ httpDependencies
  )

lazy val root = (project in file(".")).aggregate(webserver, collect, process)

/**
  以下代码需要新建plugins.sbt 放入project目录下 否则 sbt assembly不生效

resolvers += "bintray-sbt-plugins" at "http://dl.bintray.com/sbt/sbt-plugin-releases"
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
  **/