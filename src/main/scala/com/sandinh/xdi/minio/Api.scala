package com.sandinh.xdi.minio

import better.files.File
import com.sandinh.xdi.Utils
import com.typesafe.config.Config
import io.minio.MinioClient
import io.minio.errors.InvalidArgumentException

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class Api(cfg: Config) {
  private val bucket = cfg.getString("minio.bucket")
  private val client = new MinioClient(
    cfg.getString("minio.url"),
    cfg.getString("minio.key"),
    cfg.getString("minio.secret"))

  def put(objName: String, f: File, meta: Map[String, String])(implicit ec: ExecutionContext): Future[Unit] = {
    if (! f.isRegularFile) {
      Future failed new InvalidArgumentException("'" + f.pathAsString + "': not a regular file")
    } else {
      val headerMap = meta.updated("Content-Type", Utils.contentType(f)).asJava
      Future {
        for (stream <- f.inputStream) client.putObject(bucket, objName, stream, f.size, headerMap)
      }
    }
  }
}
