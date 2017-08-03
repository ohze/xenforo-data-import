package com.sandinh.ambryimport

import better.files.File
import com.sandinh.ambryimport.model.XfUser
import com.typesafe.scalalogging.Logger
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.util.{Success, Try}

class AvatarImport(boot: Boot, ambryApi: AmbryApi) {
  private val logger = Logger[AvatarImport]
  private val avatarSizes = List("l", "m", "s")
  private val LIMIT = 100

  /** @param size l|m|s */
  private def avatarFile(u: XfUser)(size: String): (String, File) = {
    val group = u.userId / 1000
    size -> File(s"${boot.rootDir}${boot.dataDir}/avatars/$size/$group/${u.userId}.jpg")
  }

  private def maybeAvatarFiles(u: XfUser): Option[List[(String, File)]] = {
    if(u.avatarDate == 0) None
    else Some(avatarSizes.map(avatarFile(u)))
  }

  import Main.ctx, ctx._
  import boot.actorSystem.dispatcher
  private val q = quote(query[XfUser])

  /**
    * @return done reason. Reason == null mean not done
    */
  private def runOne(u: XfUser): Future[String] = {
    if (u.ambry != "" && Try(Json.parse(u.ambry)).isSuccess) {
      Future successful null
    } else {
      if (u.ambry != "") logger.error(s"${u.userId}: invalid ambry field: ${u.ambry}")

      val maybeFiles = maybeAvatarFiles(u)
      if (maybeFiles.isEmpty) {
        Future successful null
      } else if (maybeFiles.get.exists { case (_, f) => !f.exists || !f.isReadable}) {
        Future successful s"avatar file not exists or not readable ${maybeFiles.get}"
      } else {
        Future.traverse(maybeFiles.get) {
          case (size, f) => ambryApi.put(f, u.userId.toString, "size" -> size).map(size -> _)
        }.flatMap { ambry =>
          val u2 = u.copy(ambry = Json.toJson(ambry).toString)
          ctx.run(q
            .filter(_.userId == lift(u.userId))
            .update(lift(u2))
          ).andThen {
            case Success(_) => logger.info(s"= ${u.userId}: ${u.ambry}")
            case _ => logger.info(s"!= ${u.userId}: ${u.ambry}/${maybeFiles.get}")
          }.map(_ => null)
        }
      }
    }
  }

  private val ReasonDoneOneBatch = "done one batch"
  /**
    * @param logic1 return done reason. Reason == null mean not done
    * @return the last processed index & doneReason
    */
  private def allBatch(offsetFrom: Int,
          logic1: XfUser => Future[String]): Future[(Int, String)] = {
    /** run from `from` to `from + LIMIT`
      * @return lastIndex -> done reason */
    def oneBatch(from: Int): Future[(Int, String)] = {
      logger.info("{}..", from)
      ctx.run(q.drop(lift(from)).take(lift(LIMIT)))
        .map(_.toArray)
        .flatMap {
          case xs if xs.isEmpty =>
            Future successful from -> "no more to process"
          case xs =>
            //run on xs.zipWithIndex with stopCondition = `i` == last item's index
            Batching.run[Int, Int](
              0,
              i => logic1(xs(i)).map {
                case null if i == xs.length - 1 => i -> ReasonDoneOneBatch
                case x => i -> x
              },
              _ + 1
            )
        }
    }

    Batching.run[Int, Int](
      offsetFrom,
      oneBatch(_).map {
        case (i, ReasonDoneOneBatch) => logger.info("!{}", i); i -> null
        case x => x
      },
      _ + 1)
  }

  def run(): Future[(Index, String)] = allBatch(0, runOne)
}
