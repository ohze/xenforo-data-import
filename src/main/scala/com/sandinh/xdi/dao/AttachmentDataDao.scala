package com.sandinh.xdi.dao

import com.sandinh.xdi.model.XfAttachmentData
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

class AttachmentDataDao(implicit cfg: Config) extends Dao[XfAttachmentData] {
  private val limit = cfg.getInt("xdi.limit")
  import com.sandinh.xdi.Main.ctx
  import ctx._
  private val q = quote(query[XfAttachmentData])

  def fetch(page: Int)(implicit ec: ExecutionContext): Future[List[XfAttachmentData]] = ctx.run(q.drop(lift(page * limit)).take(lift(limit)))
}
