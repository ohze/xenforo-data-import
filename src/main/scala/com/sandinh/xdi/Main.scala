package com.sandinh.xdi

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import com.sandinh.xdi.dao.{AttachmentDataDao, UserDao}
import com.sandinh.xdi.minio.{Api, PutStats}
import com.sandinh.xdi.model.{XfAttachmentData, XfUser}
import com.sandinh.xdi.work.{AttachmentWorker, AvatarWorker}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("xdi")
    implicit val materializer = ActorMaterializer()
    val logger = Logging(system, "xdi.Main")

    def run[T](o: Option[Batch[T]]): Future[PutStats] =
      o.fold(Future successful PutStats.Zero) { b =>
        b.source().runWith(b.sink)
      }.andThen {
        case Success(stats) => logger.info("done! {}", stats)
        case Failure(e) => logger.error("error", e)
      }

    implicit val tscfg: Config = ConfigFactory.load()
    implicit val cfg = new XdiConfig
    implicit val api = new Api

    val ava =
      if (! tscfg.getBoolean("xdi.avatar.run")) None
      else Some(new Batch[XfUser](
        new UserDao,
        new AvatarWorker,
        tscfg.getInt("xdi.avatar.from"),
        "Ava"
      ))

    val att =
      if (! tscfg.getBoolean("xdi.attachment.run")) None
      else Some(new Batch[XfAttachmentData](
        new AttachmentDataDao,
        new AttachmentWorker,
        tscfg.getInt("xdi.attachment.from"),
        "Att"
      ))

    Await.result(run(ava) zip run(att), Duration.Inf)
    system.terminate()
  }
}
