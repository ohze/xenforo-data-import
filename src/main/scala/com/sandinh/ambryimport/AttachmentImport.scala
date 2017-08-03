package com.sandinh.ambryimport

import better.files.File
import com.sandinh.ambryimport.model.{XfAttachmentData, XfUser}
import com.typesafe.scalalogging.Logger
import play.api.libs.json.Json

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Success, Try}

class AttachmentImport(boot: Boot, ambryApi: AmbryApi) {
  private val logger = Logger[AttachmentImport]
  private val LIMIT = 100


  /** see XenForo_Model_Attachment::getAttachmentDataFilePath */
  private def getAttachmentDataFilePath(d: XfAttachmentData): String = boot.rootDir + {
    if(d.filePath == null || d.filePath == "") {
      s"${boot.internalDataDir}/attachments/${d.dataId /1000}/${d.dataId}-${d.fileHash}.data"
    } else {
      Utils.strtr(d.filePath,
        "%INTERNAL%" -> boot.internalDataDir,
        "%DATA%" -> boot.dataDir,
        "%DATA_ID%" -> d.dataId.toString,
        "%FLOOR%" -> (d.dataId /1000).toString,
        "%HASH%" -> d.fileHash)
    }
  }
  private def getAttachmentThumbnailFilePath(d: XfAttachmentData) =
    s"${boot.rootDir}${boot.dataDir}/attachments/${d.dataId /1000}/${d.dataId}-${d.fileHash}.jpg"

  import Main.ctx
  import boot.actorSystem.dispatcher
  import ctx._
  private val q = quote(query[XfAttachmentData])

  /**
    * @return done reason. Reason == null mean not done
    */
  private def runOne(d: XfAttachmentData): Future[String] = {
    if (d.ambry != "" && Try(Json.parse(d.ambry)).isSuccess) {
      Future successful null
    } else {
      if (d.ambry != "") logger.error(s"${d.userId}: invalid ambry field: ${d.ambry}")
      val file = File(getAttachmentDataFilePath(d))
      if (! file.exists) {
        Future successful s"attachement file not exist: ${file.pathAsString}"
      } else if (! file.isReadable) {
        Future successful s"attachement file not readable: ${file.pathAsString}"
      } else {
        val isImage = Utils.isImage(file)
        val files = ListBuffer("data" -> file)
        if (isImage) files += "thumb" -> File(getAttachmentThumbnailFilePath(d))
        Future.traverse(files.result()) {
          case (tpe, f) =>
            val attachType = if (isImage) "inline" else "attachment"
            ambryApi.put(f, d.userId.toString,
              "-inject-Content-Disposition" -> (attachType + "; filename=\"" + f.name + "\""),
              "t" -> tpe
            ).map(tpe -> _)
        }.flatMap { ambry =>
          val d2 = d.copy(ambry = Json.toJson(ambry).toString)
          ctx.run(q
            .filter(_.dataId == lift(d.dataId))
            .update(lift(d2))
          ).andThen {
            case Success(_) => logger.info(s"= ${d.dataId}: ${d.ambry}")
            case _ => logger.info(s"!= ${d.dataId}: ${d.ambry}/${files.toMap.mapValues(_.pathAsString)}")
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
          logic1: XfAttachmentData => Future[String]): Future[(Int, String)] = {
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
