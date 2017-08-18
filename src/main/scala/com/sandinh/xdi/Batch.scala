package com.sandinh.xdi

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{Sink, Source}
import com.sandinh.xdi.dao.Dao
import com.sandinh.xdi.minio.PutStats
import com.sandinh.xdi.work.Worker

import scala.concurrent.Future

/** see http://koff.io/posts/pagination-and-streams/
  * @tparam T underlying data type, ex XfUser */
class Batch[T](dao: Dao[T], worker: Worker[T], fromPage: Int)(implicit system: ActorSystem) {
  import system.dispatcher

  private val logger = Logging(system, "xdi.Batch")
  val sink: Sink[PutStats, Future[PutStats]] = Sink.fold(PutStats.Zero) {
    case (acc, stats) => acc + stats
  }

  private val logSink = Sink.fold[(Long, Int, PutStats), PutStats]((System.currentTimeMillis(), fromPage, PutStats.Zero)) {
    case ((time, page, acc), s) =>
      val logTime = if (s != PutStats.Zero || System.currentTimeMillis() - time > 5000) {
        logger.info("{}:{}={}", page, s, acc)
        System.currentTimeMillis()
      } else {
        time
      }
      (logTime, page + 1, acc + s)
  }

  def source(): Source[PutStats, NotUsed] =
    toSource(dao.fetch)
      .mapAsync(1)(xs => Future.traverse(xs)(worker.run).map(PutStats.sum)).alsoTo(logSink)

  /**
    * Convert a data fetching function to a source of data
    * @param f data fetching function, ex [[com.sandinh.xdi.dao.UserDao#fetch(int)]]
    * @return source of batching data, ex List[XfUser]
    */
  private def toSource(f: Int => Future[List[T]]): Source[List[T], NotUsed] =
    Source.unfoldAsync(fromPage) { page =>
      f(page).map { data =>
        if (data.isEmpty) None
        else Some(page + 1, data)
      }
    }
}
