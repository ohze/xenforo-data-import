package com.sandinh.xdi

import better.files.File
import com.sksamuel.scrimage.FormatDetector

import scala.collection.mutable.ListBuffer

object Utils {
  /** http://php.net/manual/en/function.strtr.php */
  def strtr(str: String, pairs: (String, String)*): String = {
    // if (pairs.contains("")) return FALSE
    pairs.sortBy { case (k, v) => - k.length } //sort DESC by key length
      //we will split str to a list of non overlap substring
      .foldLeft[ListBuffer[(String, Boolean)]](ListBuffer(str -> false)) {
      case (b1, (k, v)) =>
        b1.foldLeft(ListBuffer.empty[(String, Boolean)]) {
          case (b2, x@(s, processed)) =>
            if (processed) b2 += x
            else {
              var i1 = 0
              var i2 = s.indexOf(k)
              while (i2 != -1 && i1 < s.length) {
                b2 += (s.substring(i1, i2) -> false) += (v -> true)
                i1 = i2 + k.length
                i2 = s.indexOf(k, i1)
              }
              b2 += (s.substring(i1) -> false)
            }
        }
    }.foldLeft(StringBuilder.newBuilder) {
      case (b, (s, _)) => b ++= s
    }.result()
  }

  def contentType(f: File): String = {
    import com.sksamuel.scrimage.Format._

    f.inputStream.map(FormatDetector.detect).collectFirst {
      case Some(PNG) => "image/png"
      case Some(JPEG) => "image/jpeg"
      case Some(GIF) => "image/gif"
    }.getOrElse("application/octet-stream")
  }

  def isImage(f: File): Boolean = f.inputStream.exists(FormatDetector.detect(_).isDefined)
}
