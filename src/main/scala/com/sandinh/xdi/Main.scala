package com.sandinh.xdi

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.sandinh.xdi.dao.{AttachmentDataDao, UserDao}
import com.sandinh.xdi.minio.{Api, PutStats}
import com.sandinh.xdi.model.{XfAttachmentData, XfUser}
import com.sandinh.xdi.work.{AttachmentWorker, AvatarWorker}
import com.typesafe.config.ConfigFactory
import io.getquill._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object Main {
  val ctx = new MysqlAsyncContext[SnakeCase]("db.default")

  def main(args: Array[String]): Unit = {
    val runAvatar = args.length == 1 && args(0) == "avatar"
    val runAttachment = args.length == 1 && args(0) == "attachment"
    if (!runAvatar && !runAttachment) {
      println(
        """usages:
          |$0 avatar
          |$0 attachment""".stripMargin)
    } else {
      implicit val system = ActorSystem("xdi")
      val logger = Logging(system, "xdi.Main")
      implicit val materializer = ActorMaterializer()
      implicit val tscfg = ConfigFactory.load()
      implicit val cfg = new XdiConfig
      implicit val api = new Api
      val batch = if (runAvatar) {
        new Batch[XfUser](new UserDao, new AvatarWorker)
      } else {
        new Batch[XfAttachmentData](new AttachmentDataDao, new AttachmentWorker)
      }
      val r = batch.source.runWith(batch.sink)
      r.onComplete {
        case Success(stats) => logger.info("done! {}", stats)
        case Failure(e) => logger.error("error", e)
      }
      Await.result(r, Duration.Inf)
      system.terminate()
    }
  }
}
