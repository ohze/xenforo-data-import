package com.sandinh.xdi.minio

import akka.actor.ActorSystem
import akka.event.Logging
import better.files.File
import com.sandinh.xdi.minio.Api.PutState
import com.sandinh.xdi.Utils
import com.typesafe.config.Config
import io.minio.{ErrorCode, MinioClient}
import io.minio.errors.{ErrorResponseException, InvalidArgumentException}
import com.sandinh.xdi.logSourceFromString
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
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
  def apply(state: PutState): PutStats = state match {
    case Api.RePut => PutStats.RePut
    case Api.NewPut => PutStats.NewPut
    case null => Zero
  }
}

object Api {
  sealed trait PutState
  case object RePut extends PutState
  case object NewPut extends PutState
}

class Api(implicit cfg: Config, system: ActorSystem) {
  import Api._
  private val logger = Logging(system, "xdi.Api")

  val bucket = cfg.getString("minio.bucket")
  val client = new MinioClient(
  cfg.getString("minio.url"),
  cfg.getString("minio.key"),
  cfg.getString("minio.secret"))

  /** future result is nullable */
  def put(objName: String, f: File, meta: Map[String, String])(implicit ec: ExecutionContext): Future[PutState] = {
    def checkAndPut(): PutState = {
      val state = Try(client.statObject(bucket, objName)) match {
        case Success(o) =>
          if (o.etag() == f.md5.toLowerCase) null else RePut
        case Failure(e: ErrorResponseException) if e.errorResponse.errorCode == ErrorCode.NO_SUCH_KEY => NewPut
        case Failure(e) => logger.error(s"!statObject $objName", e); null
      }

      if (state != null) {
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
