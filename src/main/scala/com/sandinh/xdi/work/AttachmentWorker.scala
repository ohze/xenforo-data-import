package com.sandinh.xdi.work

import akka.actor.ActorSystem
import akka.event.Logging
import better.files.File
import com.sandinh.xdi.{Utils, XdiConfig}
import com.sandinh.xdi.minio.{Api, PutStats}
import com.sandinh.xdi.model.XfAttachmentData

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class AttachmentWorker(implicit cfg: XdiConfig, api: Api, system: ActorSystem) extends Worker[XfAttachmentData] {
  private val logger = Logging(system, "xdi.Attachment")
  import system.dispatcher

  /** see XenForo_Model_Attachment::getAttachmentDataFilePath */
  private def getAttachmentDataFilePath(d: XfAttachmentData): String = cfg.rootDir + {
    if(d.filePath == null || d.filePath == "") {
      s"${cfg.internalDataDir}/attachments/${d.dataId /1000}/${d.dataId}-${d.fileHash}.data"
    } else {
      Utils.strtr(d.filePath,
        "%INTERNAL%" -> cfg.internalDataDir,
        "%DATA%" -> cfg.dataDir,
        "%DATA_ID%" -> d.dataId.toString,
        "%FLOOR%" -> (d.dataId /1000).toString,
        "%HASH%" -> d.fileHash)
    }
  }
  private def getAttachmentThumbnailFilePath(d: XfAttachmentData) =
    s"${cfg.rootDir}${cfg.dataDir}/attachments/${d.dataId /1000}/${d.dataId}-${d.fileHash}.jpg"

  def run(d: XfAttachmentData): Future[PutStats] = {
    val file = File(getAttachmentDataFilePath(d))
    val isImage = Utils.isImage(file)
    val files = ListBuffer(cfg.objName(file, internal = true) -> file)
    if (isImage) {
      val thumb = File(getAttachmentThumbnailFilePath(d))
      files += cfg.objName(thumb, internal = true) -> thumb
    }
    Future.traverse(files.result()) {
      case (objName, f) =>
        if (f.exists) {
          val attachType = if (isImage) "inline" else "attachment"
          api.put(objName, f, Map(
            "owner" -> d.userId.toString,
            "Content-Disposition" -> (attachType + "; filename=\"" + f.name + "\"")
          )).map(PutStats.apply)
        } else {
          //print("!" + Integer.toString(d.dataId, 36))
          Future successful PutStats.FileNotFound
        }
    }.map(PutStats.sum)
  }
}
