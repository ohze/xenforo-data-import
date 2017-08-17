package com.sandinh.xdi.dao

import com.sandinh.xdi.model.User
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

class UserDao(cfg: Config) extends Dao[User] {
  private val limit = cfg.getInt("xdi.limit")
  import com.sandinh.xdi.Main.ctx, ctx._
  private val q = quote(query[User])

  def fetch(page: Int)(implicit ec: ExecutionContext): Future[List[User]] = ctx.run(q.drop(lift(page * limit)).take(lift(limit)))
}
