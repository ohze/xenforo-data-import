package com.sandinh.xdi.dao

import io.getquill.{MysqlAsyncContext, SnakeCase}
import scala.concurrent.Future

object Dao {
  val ctx = new MysqlAsyncContext[SnakeCase]("db.default")
}
trait Dao[T] {
  def fetch(page: Int): Future[List[T]]
}
