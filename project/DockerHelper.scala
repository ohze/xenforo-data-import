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
    mappings <<= (mappings, name, defaultLinuxInstallLocation, streams) map { (m, n, l, s) =>
      val excludes = List(
        s"bin/$n.bat"
      ).map(f => s"$l/$f")

      m.filterNot { case (_, p) =>
        val isExclude = excludes.contains(p)
        if (isExclude)
          s.log.warn(s"docker - excluding $p")
        isExclude
      }
    }
  ))

  val dockerCommandsSetting: Def.Setting[Task[Seq[CmdLike]]] = dockerCommands in Docker <<= (
    defaultLinuxInstallLocation in Docker,
    daemonUser in Docker,
    daemonGroup in Docker,
    daemonUserUid in Docker,
    daemonGroupGid in Docker,
    dockerEntrypoint, dockerExposedPorts, dockerBaseImage, maintainer
    ) map { (dockerBaseDirectory, user, group, uId, gId, entry, ports, baseImage, m) =>

    val addCmd = {
      val dir = dockerBaseDirectory.dropWhile(_ == '/')
      Cmd("ADD", s"$dir /$dir")
    }

    val runCmd = {
      MultiRunCmd(
        s"addgroup -g ${gId.getOrElse("82")} -S $group",
        s"adduser -u ${uId.getOrElse("82")} -D -S -G $group $user",
        s"chown -R $user:$group .",
        """sed 's|^java \(.*$opts \)|java \1$JAVA_OPTS |' -i """ + entry.head
      )
    }

    val exposedCmd = {
      if (ports.isEmpty) None
      else Some(Cmd("EXPOSE", ports mkString " "))
    }

    Seq(
      Cmd("FROM", baseImage),
      Cmd("MAINTAINER", m),
      addCmd,
      Cmd("WORKDIR", dockerBaseDirectory),
      runCmd,
      Cmd("ENV", "JAVA_OPTS", defaultJavaOpts.mkString("\"", " ", "\""))) ++
      exposedCmd ++ //not have VOLUME cmd
      Seq(
        Cmd("USER", user),
        ExecCmd("CMD", entry: _*)
      )
  }
}
