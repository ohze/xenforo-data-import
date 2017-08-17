package com.sandinh.xdi

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.sandinh.xdi.dao.UserDao
import com.sandinh.xdi.minio.Api
import com.sandinh.xdi.model.User
import com.sandinh.xdi.work.AvatarWorker
import com.typesafe.scalalogging.Logger
import io.getquill._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object Main {
  private val logger = Logger[Main.type]
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
      implicit val materializer = ActorMaterializer()
      val cfg = new XdiConfig
      val batch = if (runAvatar) {
        new Batch[User](new UserDao(cfg.conf), new AvatarWorker(cfg, new Api(cfg.conf)))
      } else {
        ???
      }
      val noopSink = Sink.foreach[Unit](identity) //do nothing
      val r = batch.source.runWith(noopSink)
      r.onComplete {
        case Success(_) => logger.info("done!")
        case Failure(e) => logger.error("error", e)
      }
      Await.result(r, Duration.Inf)
      system.terminate()
    }
  }
}
