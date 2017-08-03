package com.sandinh.ambryimport

import io.getquill._
import com.typesafe.scalalogging.Logger
import scala.concurrent.ExecutionContext.Implicits.global
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
      val boot = new Boot
      val ambryApi = new AmbryApi(boot)
      val f =
        if (runAvatar) new AvatarImport(boot, ambryApi).run()
        else new AttachmentImport(boot, ambryApi).run()

      f.onComplete {
        case Success((lastOffset, doneReason)) => logger.info(s"done at $lastOffset: $doneReason")
        case Failure(e) => logger.error("error", e)
      }
      while (!f.isCompleted) Thread.sleep(300) //FIXME
    }
  }
}
