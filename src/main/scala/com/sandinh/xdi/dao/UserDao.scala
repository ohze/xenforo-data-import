package com.sandinh.xdi.dao

import com.sandinh.xdi.model.XfUser
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

class UserDao(implicit cfg: Config, ec: ExecutionContext) extends Dao[XfUser] {
  private val limit = cfg.getInt("xdi.limit")
  import Dao.ctx, ctx._
  private val q = quote(query[XfUser])

  def fetch(page: Int): Future[List[XfUser]] = ctx.run(q.drop(lift(page * limit)).take(lift(limit)))
}
