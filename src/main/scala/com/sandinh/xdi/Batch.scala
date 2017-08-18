package com.sandinh.xdi

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{Sink, Source}
import com.sandinh.xdi.dao.Dao
import com.sandinh.xdi.minio.PutStats
import com.sandinh.xdi.work.Worker

import scala.concurrent.{ExecutionContext, Future}

/** see http://koff.io/posts/pagination-and-streams/
  * @tparam T underlying data type, ex XfUser */
class Batch[T](dao: Dao[T], worker: Worker[T])(implicit system: ActorSystem) {
  private val logger = Logging(system, "xdi.Batch")
  val sink: Sink[PutStats, Future[PutStats]] = Sink.fold(PutStats.Zero) {
    case (acc, stats) => acc + stats
  }

  private val logSink = Sink.fold[(Int, PutStats), PutStats](0 -> PutStats.Zero) {
    case ((page, acc), s) =>
      logger.info("{}:{}={}", page, s, acc)
      (page + 1, acc + s)
  }

  def source(implicit ec: ExecutionContext): Source[PutStats, NotUsed] =
    toSource(dao.fetch)
      .mapAsync(1)(xs => Future.traverse(xs)(worker.run).map(PutStats.sum)).alsoTo(logSink)

  /**
    * Convert a data fetching function to a source of data
    * @param f data fetching function, ex [[com.sandinh.xdi.dao.UserDao#fetch(int)]]
    * @return source of batching data, ex List[XfUser]
    */
  private def toSource(f: Int => Future[List[T]])(implicit ec: ExecutionContext): Source[List[T], NotUsed] =
    Source.unfoldAsync(0) { page =>
      f(page).map { data =>
        if (data.isEmpty) None
        else Some(page + 1, data)
      }
    }
}
