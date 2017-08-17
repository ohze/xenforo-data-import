package com.sandinh.xdi

import org.scalatest._

class UtilsSpec extends FlatSpec with Matchers {
  "Utils.strtr" should "correct" in {
    Utils.strtr("hi all, I said hello",
      "h" -> "-",
      "hello" -> "hi",
      "hi" -> "hello")  should be ("hello all, I said hi")

    Utils.strtr("hi all, hello, Hello hi hoho I said hello",
      "h" -> "-",
      "hello" -> "hi",
      "hi" -> "hello")  should be ("hello all, hi, Hello hello -o-o I said hi")
  }
}
