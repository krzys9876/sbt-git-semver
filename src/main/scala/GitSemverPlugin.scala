import sbt.*
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import complete.DefaultParsers.*
import sbt.Keys.streams

/**
 * A main object of the plugin, a wrapper for commands
 */
object GitSemverPlugin extends AutoPlugin {

  /**
   * A declaration of the sbt command
   */
  object autoImport {
    val gitSemverNextVersion = inputKey[Unit](
      "Calculates next version from git tags matching major.minor.patch, saves it to file and pushes new tag to repo")
  }

  private lazy val handler:GitHandler = GitHandler()

  import autoImport.*

  override def trigger = allRequirements
  /**
   *  Actual implementation of the sbt command
   *  The GitHandler instance is used to initialize GitVersions with repo parameters and determine the next version
   */
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
