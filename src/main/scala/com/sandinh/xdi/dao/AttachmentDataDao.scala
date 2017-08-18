package com.sandinh.xdi.dao

import com.sandinh.xdi.model.AttachmentData
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

class AttachmentDataDao(cfg: Config) extends Dao[AttachmentData] {
  private val limit = cfg.getInt("xdi.limit")
  import com.sandinh.xdi.Main.ctx
  import ctx._
  private val q = quote(query[AttachmentData])

  def fetch(page: Int)(implicit ec: ExecutionContext): Future[List[AttachmentData]] = ctx.run(q.drop(lift(page * limit)).take(lift(limit)))
}
