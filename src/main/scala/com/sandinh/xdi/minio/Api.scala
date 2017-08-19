package com.sandinh.xdi.minio

import akka.actor.ActorSystem
import akka.event.Logging
import better.files.File
import com.sandinh.xdi.Utils
import com.typesafe.config.Config
import io.minio.{ErrorCode, MinioClient}
import io.minio.errors.{ErrorResponseException, InvalidArgumentException}
import com.sandinh.xdi.logSourceFromString
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class PutStats(rePut: Int, newPut: Int, fileNotFound: Int) {
  def +(b: PutStats): PutStats = PutStats(rePut + b.rePut, newPut + b.newPut, fileNotFound + b.fileNotFound)

  override def toString: String = s"$rePut,$newPut,$fileNotFound"
}
object PutStats {
  val Zero = PutStats(0, 0, 0)
  val RePut = PutStats(1, 0, 0)
  val NewPut = PutStats(0, 1, 0)
  val FileNotFound = PutStats(0, 0, 1)
  def sum(l: List[PutStats]): PutStats = l.reduce(_ + _)
}

class Api(implicit cfg: Config, system: ActorSystem) {
  import system.dispatcher
  private val logger = Logging(system, "xdi.Api")

  val bucket: String = cfg.getString("minio.bucket")
  val client = new MinioClient(
  cfg.getString("minio.url"),
  cfg.getString("minio.key"),
  cfg.getString("minio.secret"))

  def put(objName: String, f: File, meta: Map[String, String]): Future[PutStats] = {
    def checkAndPut(): PutStats = {
      val state = Try(client.statObject(bucket, objName)) match {
        case Success(o) =>
          if (o.etag() == f.md5.toLowerCase) PutStats.Zero else PutStats.RePut
        case Failure(e: ErrorResponseException) if e.errorResponse.errorCode == ErrorCode.NO_SUCH_KEY => PutStats.NewPut
        case Failure(e) => logger.error("!statObject {} {}", objName, e); PutStats.Zero
      }

      if (state != PutStats.Zero) {
        logger.info(s"$state $objName")
        val headerMap = meta.updated("Content-Type", Utils.contentType(f)).asJava
        for (stream <- f.inputStream) client.putObject(bucket, objName, stream, f.size, headerMap)
      }
      state
    }

    if (! f.isRegularFile) {
      Future failed new InvalidArgumentException("'" + f.pathAsString + "': not a regular file")
    } else {
      Future(checkAndPut())
    }
  }
}
