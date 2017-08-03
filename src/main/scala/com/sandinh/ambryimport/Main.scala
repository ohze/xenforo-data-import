package com.sandinh.ambryimport

import io.getquill._
import scala.concurrent.duration._
import scala.concurrent.Await
import com.typesafe.scalalogging.Logger

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
      val boot = new Boot
      import boot.actorSystem.dispatcher
      val ambryApi = new AmbryApi(boot)
      val f =
        if (runAvatar) new AvatarImport(boot, ambryApi).run()
        else new AttachmentImport(boot, ambryApi).run()

      while (!f.isCompleted) Thread.sleep(300)
      val (lastOffset, doneReason) = Await.result(f, Duration.Zero)
      logger.info(s"done at $lastOffset: $doneReason")
    }
  }
}
