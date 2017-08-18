package com.sandinh.xdi.work

import better.files.File
import com.sandinh.xdi.{Utils, XdiConfig}
import com.sandinh.xdi.minio.Api
import com.sandinh.xdi.model.XfAttachmentData

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class AttachmentWorker(cfg: XdiConfig, api: Api) extends Worker[XfAttachmentData] {
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

  def run(d: XfAttachmentData)(implicit ec: ExecutionContext): Future[Unit] = {
    val file = File(getAttachmentDataFilePath(d))
    val isImage = Utils.isImage(file)
    val files = ListBuffer(cfg.objName(file, internal = true) -> file)
    if (isImage) {
      val thumb = File(getAttachmentThumbnailFilePath(d))
      files += cfg.objName(thumb, internal = true) -> thumb
    }
    Future.traverse(files.result()) {
      case (objName, f) =>
        val attachType = if (isImage) "inline" else "attachment"
        api.put(objName, f, Map(
          "owner" -> d.userId.toString,
          "Content-Disposition" -> (attachType + "; filename=\"" + f.name + "\"")
        ))
    }.map(_ => Unit)
  }
}
