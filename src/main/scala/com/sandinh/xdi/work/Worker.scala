package com.sandinh.xdi.work

import scala.concurrent.{ExecutionContext, Future}

trait Worker[T] {
  def run(d: T)(implicit ec: ExecutionContext): Future[Unit]
}
