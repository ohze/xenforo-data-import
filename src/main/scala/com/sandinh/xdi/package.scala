package com.sandinh

import akka.actor.ActorSystem
import akka.event.{DummyClassForStringSources, LogSource}

package object xdi {
  implicit val logSourceFromString: LogSource[String] = new LogSource[String] {
    def genString(s: String) = s
    override def genString(s: String, system: ActorSystem) = s //+ "(" + system + ")"
    override def getClazz(s: String) = classOf[DummyClassForStringSources]
  }
}
