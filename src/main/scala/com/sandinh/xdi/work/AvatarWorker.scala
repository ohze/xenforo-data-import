package com.sandinh.xdi.work

import better.files.File
import com.sandinh.xdi.XdiConfig
import com.sandinh.xdi.minio.Api
import com.sandinh.xdi.model.XfUser
import scala.concurrent.{ExecutionContext, Future}

class AvatarWorker(cfg: XdiConfig, api: Api) extends Worker[XfUser] {
  private val sizes = List("l", "m", "s")
  private def avatarFile(u: XfUser, size: String): File = {
    val group = u.userId / 1000
    File(s"${cfg.rootDir}${cfg.dataDir}/avatars/$size/$group/${u.userId}.jpg")
  }

  def run(u: XfUser)(implicit ec: ExecutionContext): Future[Unit] = {
    if (u.avatarDate == 0) Future successful Unit
    else {
      Future.traverse(sizes) { size =>
        val f = avatarFile(u, size)
        api.put(cfg.objName(f, internal = false), f, Map.empty[String, String])
      }.map(_ => Unit)
    }
  }
}
