package com.sandinh.xdi.dao

import com.sandinh.xdi.model.XfUser
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

class UserDao(cfg: Config) extends Dao[XfUser] {
  private val limit = cfg.getInt("xdi.limit")
  import com.sandinh.xdi.Main.ctx, ctx._
  private val q = quote(query[XfUser])

  def fetch(page: Int)(implicit ec: ExecutionContext): Future[List[XfUser]] = ctx.run(q.drop(lift(page * limit)).take(lift(limit)))
}
