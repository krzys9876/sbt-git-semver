import sbt.*

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import complete.DefaultParsers.*
import sbt.Keys.streams
import sbt.internal.util.ManagedLogger

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
    val gitSemverNextVersionMain = inputKey[Unit](
      "Calculates next version from git tags matching major.minor.patch, saves it to file and pushes new tag to repo. Returns version for main branch.")
    val gitSemverNextVersionSnapshot = inputKey[Unit](
      "Calculates next version from git tags matching major.minor.patch, saves it to file and pushes new tag to repo. Returns snapshot version.")
  }

  import autoImport.*

  override def trigger = allRequirements
  /**
   *  Actual implementation of the sbt command
   *  The GitHandler instance is used to initialize GitVersions with repo parameters and determine the next version
   */
  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    gitSemverNextVersion :=
      handleCommand(GitHandler(),spaceDelimited("<arg>").parsed.headOption,Some(streams.value.log)),
    gitSemverNextVersionMain :=
      handleCommand(GitHandler(Some(VersionType.MAIN)), spaceDelimited("<arg>").parsed.headOption,Some(streams.value.log)),
    gitSemverNextVersionSnapshot :=
      handleCommand(GitHandler(Some(VersionType.SNAPSHOT)), spaceDelimited("<arg>").parsed.headOption,Some(streams.value.log)))

  def handleCommand(handler:GitHandler,filename:Option[String],log:Option[ManagedLogger],doPush:Boolean=true):Unit = {
    val nextVersion = handler.nextVersion
    log.foreach(_.info(nextVersion.toString))
    saveToFile(handler.repoPath,nextVersion.toString,filename)
    handler.pushTagIfChanged(doPush)
  }
  private def saveToFile(repoPath:Option[File],version:String,fileName:Option[String]):Unit = {
    val actualPath=repoPath.map(_.getAbsolutePath).getOrElse("")
    fileName.foreach(f => Files.write( Paths.get(actualPath,f), version.getBytes(StandardCharsets.UTF_8)))
  }
}
