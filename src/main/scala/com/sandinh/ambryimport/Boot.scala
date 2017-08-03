package com.sandinh.ambryimport

import com.typesafe.config.{Config, ConfigFactory}
import gigahorse.HttpClient
import gigahorse.support.okhttp.Gigahorse

class Boot {
  val conf: Config = ConfigFactory.load()
  val http: HttpClient = Gigahorse.http(Gigahorse.config)
  val rootDir: String = conf.getString("xf.dir.root")
  val dataDir: String = conf.getString("xf.dir.data")
  val internalDataDir: String = conf.getString("xf.dir.internal")

  scala.sys.addShutdownHook {
    http.close()
  }
}
