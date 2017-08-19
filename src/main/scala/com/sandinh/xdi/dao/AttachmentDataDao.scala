package com.sandinh.xdi.dao

import com.sandinh.xdi.model.XfAttachmentData
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

class AttachmentDataDao(implicit cfg: Config, ec: ExecutionContext) extends Dao[XfAttachmentData] {
  private val limit = cfg.getInt("xdi.limit")
  import Dao.ctx, ctx._
  private val q = quote(query[XfAttachmentData])

  def fetch(page: Int): Future[List[XfAttachmentData]] = ctx.run(q.drop(lift(page * limit)).take(lift(limit)))
}
