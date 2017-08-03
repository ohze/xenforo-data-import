package com.sandinh.ambryimport

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import gigahorse.ReactiveHttpClient
import gigahorse.support.akkahttp.Gigahorse
import scala.concurrent.Await
import scala.concurrent.duration._

class Boot {
  val conf: Config = ConfigFactory.load()
  implicit val actorSystem = ActorSystem("gigahorse-akka-http", conf)
  implicit val materializer = ActorMaterializer()
  val http: ReactiveHttpClient = Gigahorse.http(Gigahorse.config, actorSystem)
  val rootDir: String = conf.getString("xf.dir.root")
  val dataDir: String = conf.getString("xf.dir.data")
  val internalDataDir: String = conf.getString("xf.dir.internal")

  scala.sys.addShutdownHook {
    http.close()
    Await.result(actorSystem.terminate(), 30.seconds)
  }
}
