lazy val coreSettings = Seq(
  name := "xenforo-data-import",
  organization := "com.sandinh",
  scalaVersion := "2.12.3",
  version      := "1.0.0",
  scalacOptions ++= Seq("-encoding", "UTF-8")
)

lazy val dependencies = Seq(
  "com.sandinh" % "minio" % "3.0.7", //TODO use io.minio:minio when version 3.0.7 available
  "com.typesafe.akka" %% "akka-actor" % "2.5.4",
  "com.typesafe.akka" %% "akka-stream" % "2.5.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.4",
  "org.scalatest" %% "scalatest" % "3.0.3" % Test,
  "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8",
  "com.github.pathikrit" %% "better-files" % "3.0.0",
  "io.getquill" %% "quill-async-mysql" % "1.3.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

lazy val dependencySettings = Seq(
  resolvers += Resolver.sonatypeRepo("releases"),
  libraryDependencies ++= dependencies
)

lazy val packagerSettings = Seq(
  dockerUsername := Some("sandinh"),
  packageName in Docker := "xenforo-data-import",
  daemonUser in Docker := "xdi",
  daemonUserUid in Docker := Some("82"),
  daemonGroup in Docker := "www-data",
  daemonGroupGid in Docker := Some("82"),
  maintainer := "Gia Bao <giabao@sandinh.net>",
  dockerBaseImage := "openjdk:8-jre-alpine",
  DockerHelper.dockerCommandsSetting
) ++ DockerHelper.mappingsSettings

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin, AshScriptPlugin)
  .settings(
    coreSettings ++
    dependencySettings ++
    packagerSettings: _*
  )
