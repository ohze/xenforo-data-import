package com.sandinh.xdi

import better.files.File
import com.typesafe.config.Config

class XdiConfig(implicit conf: Config) {
  val rootDir: String = conf.getString("xf.dir.root")
  val dataDir: String = conf.getString("xf.dir.data")
  val internalDataDir: String = conf.getString("xf.dir.internal")
  private val dataPathLen = rootDir.length + dataDir.length
  private val internalDataPathLen = rootDir.length + internalDataDir.length

  def objName(f: File, internal: Boolean): String =
    if (internal) {
      "internal_data" + f.pathAsString.substring(internalDataPathLen)
    } else {
      "data" + f.pathAsString.substring(dataPathLen)
    }
}
