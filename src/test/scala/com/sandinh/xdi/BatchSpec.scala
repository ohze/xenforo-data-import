package com.sandinh.xdi

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import com.sandinh.xdi.dao.Dao
import com.sandinh.xdi.minio.PutStats
import com.sandinh.xdi.work.Worker
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object BatchSpec {
  type T=Int
  object TestDao {
    val lastPage = 6
    val fetchRet = List(3,1,1, 0)
  }
  class TestDao extends Dao[T] { import TestDao._
    def fetch(page: Int)(implicit ec: ExecutionContext): Future[List[T]] = {
      if (page <= lastPage) Future successful fetchRet.map(_ + page * 5)
      else Future successful Nil
    }
  }
  object TestWorker {
    val runRet = PutStats(3, 2, 5)
  }
  class TestWorker extends Worker[T] { import TestWorker._
    def run(d: T): Future[PutStats] = Future successful runRet
  }
  implicit class PutStatsOps(val a: PutStats) extends AnyVal {
    def *(n: Int): PutStats = PutStats(a.rePut * n, a.newPut * n, a.fileNotFound * n)
  }
}

class BatchSpec extends FlatSpec with Matchers {
  import BatchSpec._
  "Batch" should "correct" in {
    implicit val system = ActorSystem("xdi_")
    implicit val materializer = ActorMaterializer()
    implicit val tscfg: Config = ConfigFactory.load()
    val fromPage = 2
    val batch = new Batch[T](new TestDao, new TestWorker, fromPage)
    val rf = batch.source().runWith(batch.sink)
    val r = Await.result(rf, Duration.Inf)
    r shouldEqual TestWorker.runRet * TestDao.fetchRet.length * (TestDao.lastPage + 1 - fromPage)
    system.terminate()
  }
}
