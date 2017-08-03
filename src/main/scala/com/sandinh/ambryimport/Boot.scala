package com.sandinh.ambryimport

import com.typesafe.config.{Config, ConfigFactory}

class Boot {
  val conf: Config = ConfigFactory.load()
  val rootDir: String = conf.getString("xf.dir.root")
  val dataDir: String = conf.getString("xf.dir.data")
  val internalDataDir: String = conf.getString("xf.dir.internal")
}
