package com.sandinh.xdi.work

import com.sandinh.xdi.minio.PutStats

import scala.concurrent.Future

trait Worker[T] {
  def run(d: T): Future[PutStats]
}
