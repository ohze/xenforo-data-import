package com.sandinh.xdi.dao

import scala.concurrent.{ExecutionContext, Future}

trait Dao[T] {
  def fetch(page: Int)(implicit ec: ExecutionContext): Future[List[T]]
}
