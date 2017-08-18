package com.sandinh.xdi.work

import akka.actor.ActorSystem
import akka.event.Logging
import better.files.File
import com.sandinh.xdi.XdiConfig
import com.sandinh.xdi.minio.{Api, PutStats}
import com.sandinh.xdi.model.XfUser

import scala.concurrent.Future

class AvatarWorker(implicit cfg: XdiConfig, api: Api, system: ActorSystem) extends Worker[XfUser] {
  import system.dispatcher
  private val logger = Logging(system, "xdi.Avatar")
  private val sizes = List("l", "m", "s")
  private def avatarFile(u: XfUser, size: String): File = {
    val group = u.userId / 1000
    File(s"${cfg.rootDir}${cfg.dataDir}/avatars/$size/$group/${u.userId}.jpg")
  }

  def run(u: XfUser): Future[PutStats] = {
    if (u.avatarDate == 0) Future successful PutStats.Zero
    else {
      Future.traverse(sizes) { size =>
        val f = avatarFile(u, size)
        val objName = cfg.objName(f, internal = false)
        if (f.exists) {
          api.put(objName, f, Map.empty[String, String]).map(PutStats.apply)
        } else {
          //print("!" + Integer.toString(u.userId, 36))
          //logger.warning("!file {}", objName)
          Future successful PutStats.FileNotFound
        }
      }.map(PutStats.sum)
    }
  }
}
