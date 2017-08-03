package com.sandinh.ambryimport

import scala.concurrent.{ExecutionContext, Future}

object Batching {
  /**
    * @param logic return B -> done reason. Reason == null mean not done
    * @return the last processed B & doneReason
    */
  def run[A, B](from: A,
                logic: A => Future[(B, String)],
                next: B => A)(implicit ec: ExecutionContext): Future[(B, String)] = {
    logic(from).flatMap {
      case r@(b, doneReason) =>
        if (doneReason != null) Future successful r
        else run(next(b), logic, next)
    }
  }
}
