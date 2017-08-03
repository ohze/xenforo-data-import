package com.sandinh.ambryimport

import better.files._
import java.nio.file.Files
import com.sksamuel.scrimage.FormatDetector
import gigahorse._
import support.akkahttp.Gigahorse

import scala.concurrent.Future

class AmbryApi(boot: Boot) {
  import boot.http, boot.actorSystem.dispatcher

  private val AuthorizeHeader = "Authorization" -> boot.conf.getString("ambry.header.authorize")
  private val ServiceIdHeader = "x-ambry-service-id" -> boot.conf.getString("ambry.header.serviceid")
  private val ambryUrl = boot.conf.getString("ambry.url")

  private def contentType(f: File): String = {
    import com.sksamuel.scrimage.Format._

    f.inputStream.map(FormatDetector.detect).collectFirst {
      case Some(PNG) => "image/png"
      case Some(JPEG) => "image/jpeg"
      case Some(GIF) => "image/gif"
    }.getOrElse("application/octet-stream")
  }

  def put(file: File, maybeOwner: Option[String], extra: (String, String)*): Future[String] = {
    val req = maybeOwner.foldLeft(
      Gigahorse.url(ambryUrl)
        .withHeaders(
          AuthorizeHeader,
          ServiceIdHeader,
          "x-ambry-blob-size" -> Files.size(file.path).toString,
          "x-ambry-content-type" -> contentType(file)
        ).withHeaders(extra.map { case (k, v) => "x-ambry-um-" + k -> v }: _*)
        .post(file.toJava)
    ) { case (r, owner) => r.withHeaders("x-ambry-owner-id" -> owner)}

    http.run(req).flatMap { res =>
      if(res.status != 201) {
        Future failed new Exception(s"Put ${file.pathAsString} error: ${res.status}")
      } else {
        Future successful res.header("Location").get.substring(1)
      }
    }
  }
}
