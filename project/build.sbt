addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC10")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.2-RC2")
//addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.1.0")

//https://github.com/coursier/coursier/issues/450#issuecomment-302267082
classpathTypes += "maven-plugin"
