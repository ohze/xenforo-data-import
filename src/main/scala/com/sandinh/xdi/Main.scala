package com.sandinh.xdi

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import com.sandinh.xdi.dao.{AttachmentDataDao, UserDao}
import com.sandinh.xdi.minio.Api
import com.sandinh.xdi.model.{XfAttachmentData, XfUser}
import com.sandinh.xdi.work.{AttachmentWorker, AvatarWorker}
import com.typesafe.config.{Config, ConfigFactory}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("xdi")
    val logger = Logging(system, "xdi.Main")
    implicit val materializer = ActorMaterializer()
    implicit val tscfg: Config = ConfigFactory.load()
    implicit val cfg = new XdiConfig
    implicit val api = new Api
    val fromPage = tscfg.getInt("xdi.from")
    val batch = if (tscfg.getString("xdi.run") == "avatar") {
      new Batch[XfUser](new UserDao, new AvatarWorker, fromPage)
    } else {
      new Batch[XfAttachmentData](new AttachmentDataDao, new AttachmentWorker, fromPage)
    }
    val r = batch.source().runWith(batch.sink)
    r.onComplete {
      case Success(stats) => logger.info("done! {}", stats)
      case Failure(e) => logger.error("error", e)
    }
    Await.result(r, Duration.Inf)
    system.terminate()
  }
}
