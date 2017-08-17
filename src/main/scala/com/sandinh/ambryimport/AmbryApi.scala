package com.sandinh.ambryimport

import java.nio.file.Files

import better.files._
import com.sandinh.xdi.{Utils, XdiConfig}
import com.softwaremill.sttp._
import com.softwaremill.sttp.okhttp.OkHttpFutureClientHandler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmbryApi(boot: XdiConfig) {
  private val AuthorizeHeader = "Authorization" -> boot.conf.getString("ambry.header.authorize")
  private val ServiceIdHeader = "x-ambry-service-id" -> boot.conf.getString("ambry.header.serviceid")
  private val ambryUrl = boot.conf.getString("ambry.url")

  @inline final def put(file: File, owner: String, extra: (String, String)*): Future[String] =
    put(file, owner, Utils.contentType(file), extra: _*)

  @inline final def put(file: File, owner: String, mimeType: String,  extra: (String, String)*): Future[String] =
    put(file, Some(owner), mimeType, extra: _*)

  def put(file: File, maybeOwner: Option[String], mimeType: String, extra: (String, String)*): Future[String] = {
    val req = maybeOwner.foldLeft(
      sttp.body(file.toJava)
        .headers(
          AuthorizeHeader,
          ServiceIdHeader,
          "x-ambry-blob-size" -> Files.size(file.path).toString,
          "x-ambry-content-type" -> mimeType
        ).headers(extra.map { case (k, v) => "x-ambry-um-" + k -> v }: _*)
        .post(uri"$ambryUrl")
    ) { case (r, owner) => r.headers("x-ambry-owner-id" -> owner)}
    implicit val handler = OkHttpFutureClientHandler()
    req.send().flatMap { res =>
      if(res.code != 201) {
        Future failed new Exception(s"Put ${file.pathAsString} error: ${res.body}")
      } else {
        Future successful res.header("Location").get.substring(1)
      }
    }
  }
}
