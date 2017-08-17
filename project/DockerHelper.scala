import sbt.{Def, _}
import sbt.Keys._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker

case class MultiRunCmd(cmds: String*) extends CmdLike {
  def makeContent: String = cmds.mkString("RUN \\\n  ", " && \\\n  ", "\n")
}

object DockerHelper {
  private val defaultJavaOpts = Seq("-Xms512M", "-Xmx512M")

  val mappingsSettings: Seq[Def.Setting[_]] = inConfig(Docker)(Seq(
    mappings := {
      def isExclude(p: String) = p == defaultLinuxInstallLocation.value + "/bin/" + name.value + ".bat"
      def warn(msg: String) = streams.value.log.warn(msg)

      mappings.value.filterNot { case (_, p) =>
        if (isExclude(p)) {
          warn(s"docker - excluding $p")
          true
        } else {
          false
        }
      }
    }
  ))

  val dockerCommandsSetting: Def.Setting[Task[Seq[CmdLike]]] = dockerCommands in Docker := {
    val dockerBaseDirectory = (defaultLinuxInstallLocation in Docker).value
    val addCmd = {
      val dir = dockerBaseDirectory.dropWhile(_ == '/')
      Cmd("ADD", s"$dir /$dir")
    }
    val gId = (daemonGroupGid in Docker).value.getOrElse("82")
    val uId = (daemonUserUid in Docker).value.getOrElse("82")
    val group = (daemonGroup in Docker).value
    val user = (daemonUser in Docker).value
    val entry = dockerEntrypoint.value

    val runCmd = {
      MultiRunCmd(
        s"addgroup -g $gId -S $group",
        s"adduser -u $uId -D -S -G $group $user",
        s"chown -R $user:$group .",
        """sed 's|^java \(.*$opts \)|java \1$JAVA_OPTS |' -i """ + entry.head
      )
    }

    Seq(
      Cmd("FROM", dockerBaseImage.value),
      Cmd("MAINTAINER", maintainer.value),
      addCmd,
      Cmd("WORKDIR", dockerBaseDirectory),
      runCmd,
      Cmd("ENV", "JAVA_OPTS", defaultJavaOpts.mkString("\"", " ", "\""))) ++
      Seq(
        Cmd("USER", user),
        ExecCmd("CMD", entry: _*)
      )
  }
}
