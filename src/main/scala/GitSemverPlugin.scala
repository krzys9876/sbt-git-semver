import sbt.*
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import complete.DefaultParsers.*
import sbt.Keys.streams

object GitSemverPlugin extends AutoPlugin {

  object autoImport {
    val gitSemverNextVersion = inputKey[Unit](
      "Calculates next version from git tags matching major.minor.patch, saves it to file and pushes new tag to repo")
  }

  private lazy val handler:GitHandler = GitHandler()

  import autoImport.*

  override def trigger = allRequirements
  override def projectSettings: Seq[Def.Setting[?]] = {
    gitSemverNextVersion := {
      val nextVersion = handler.versions.next
      streams.value.log.info(nextVersion.toString)
      val optionalFilename=spaceDelimited("<arg>").parsed.headOption
      optionalFilename.foreach(f=>Files.write(Paths.get(f), nextVersion.toString.getBytes(StandardCharsets.UTF_8)))
      handler.pushTagIfChanged()
      }
  }
}
