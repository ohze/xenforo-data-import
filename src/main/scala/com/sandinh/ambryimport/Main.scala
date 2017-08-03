package com.sandinh.ambryimport

import io.getquill._
import scala.concurrent.duration._
import scala.concurrent.Await
import com.typesafe.scalalogging.Logger

object Main {
  private val logger = Logger[Main.type]
  val ctx = new MysqlAsyncContext[SnakeCase]("db.default")

  def main(args: Array[String]): Unit = {
    val boot = new Boot
    import boot.actorSystem.dispatcher
    val ambryApi = new AmbryApi(boot)
    val avatarImport = new AvatarImport(boot, ambryApi)
    val f = avatarImport.run()
    while (! f.isCompleted) Thread.sleep(300)
    val (lastOffset, doneReason) = Await.result(f, Duration.Zero)
    logger.info(s"done at $lastOffset: $doneReason")
  }
}
