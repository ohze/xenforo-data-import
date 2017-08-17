package com.sandinh.xdi.model

case class AttachmentData(dataId: Int,
                          userId: Int,
                          uploadDate: Int,
                          filename: String,
                          fileSize: Int, fileHash: String,
                          filePath: String,
                          width: Int,
                          height: Int,
                          thumbnailWidth: Int,
                          thumbnailHeight: Int,
                          attachCount: Int,
                          ambry: String)

case class AmbryAttachment(data: String, thumb: Option[String])
object AmbryAttachment {
  def apply(ambry: List[(String, String)]): AmbryAttachment = {
    val a = ambry.toMap
    AmbryAttachment(a("data"), a.get("thumb"))
  }

  import play.api.libs.json._
  implicit val writer = Json.writes[AmbryAttachment]
}