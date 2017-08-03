lazy val coreSettings = Seq(
  name := "ambry-import-xenforo",
  organization := "com.sandinh",
  scalaVersion := "2.12.3",
  version      := "0.1.0-SNAPSHOT",
  scalacOptions ++= Seq("-encoding", "UTF-8")
)

lazy val dependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.3" % "test",
  "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8",
  "com.typesafe.play" %% "play-json" % "2.6.2",
  "com.eed3si9n" %% "gigahorse-akka-http" % "0.3.1",
  "com.github.pathikrit" %% "better-files" % "3.0.0",
  "io.getquill" %% "quill-async-mysql" % "1.3.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

lazy val dependencySettings = Seq(
  libraryDependencies ++= dependencies
)

lazy val packagerSettings = Seq(
  dockerUsername := Some("sandinh"),
  packageName in Docker := "ambry-import-xenforo",
  dockerUpdateLatest in Docker := true,
  daemonUser in Docker := "ambry",
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
