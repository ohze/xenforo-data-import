package com.sandinh.xdi.minio

import better.files.File
import com.sandinh.xdi.Utils
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import io.minio.{ErrorCode, MinioClient}
import io.minio.errors.{ErrorResponseException, InvalidArgumentException}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class Api(cfg: Config) {
  private val logger = Logger(getClass)

  val bucket = cfg.getString("minio.bucket")
  val client = new MinioClient(
    cfg.getString("minio.url"),
    cfg.getString("minio.key"),
    cfg.getString("minio.secret"))

  def put(objName: String, f: File, meta: Map[String, String])(implicit ec: ExecutionContext): Future[Unit] = {
    def checkAndPut() = {
      val needPut = Try(client.statObject(bucket, objName))
        .map {o =>
          val diff = o.etag() != f.md5.toLowerCase
          if (diff) logger.info(s"+re-put $objName")
          else logger.info(s"-skip $objName")
          diff
        }.recover {
          case e: ErrorResponseException if e.errorResponse.errorCode != ErrorCode.NO_SUCH_KEY =>
            logger.error(s"!statObject $objName", e)
            false
        }.getOrElse(false)

      if (needPut) {
        val headerMap = meta.updated("Content-Type", Utils.contentType(f)).asJava
        for (stream <- f.inputStream) client.putObject(bucket, objName, stream, f.size, headerMap)
      }
    }

    if (! f.isRegularFile) {
      Future failed new InvalidArgumentException("'" + f.pathAsString + "': not a regular file")
    } else {
      Future(checkAndPut())
    }
  }
}
