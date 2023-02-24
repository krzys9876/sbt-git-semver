import java.io.File
import scala.sys.process.Process

/**
 * Implements set of operations on git repository, passes the git parameters to GitVersions instance, which implements
 * the actual versioning logic
 */
case class GitHandler(overrideVersionType:Option[VersionType]=None,repoPath:Option[File]=None) {
  private lazy val versions:GitVersions = GitVersions(tags,describedTag,comment,versionType,hash)

  def nextVersion: GitVersion = versions.next
  def pushTagIfChanged(doPush:Boolean=true): Unit =
    if(!versions.isSame) {
    Process(f"git tag ${versions.next.toString}",repoPath).!
    if(doPush) Process("git push --tags",repoPath).!
  }

  private lazy val comment: String = Process("git log -1 --pretty=%B",repoPath).lineStream_!.toList.mkString(" ")
  private lazy val tags: List[String] = {
    Process("git pull --tags",repoPath).! // this makes sense only on master...
    Process("git tag",repoPath).lineStream_!.toList
  }
  private lazy val describedTag: String = Process("git describe --tags",repoPath).lineStream_!.toList.headOption.getOrElse("")
  private lazy val versionType:VersionType =
    overrideVersionType.getOrElse(
      versionTypeByComment.getOrElse(
        if(isMainBranchCommand1 || isMainBranchCommand2 || isMainBranchCommand3) VersionType.MAIN
        else VersionType.SNAPSHOT))

  private def isMainBranchCommand1:Boolean = {
    // from some thread on StackOverflow
    val branch = Process("git symbolic-ref --short HEAD",repoPath).lineStream_!.headOption.getOrElse("")
    List("main","master").contains(branch)
  }
  private def isMainBranchCommand2: Boolean = {
    // in Jenkins (observed in one production environment, git 1.8) it returns: remotes/origin/master
    val branch =
      Process("git name-rev --name-only HEAD",repoPath).lineStream_!.headOption.getOrElse("")
        .split('/').toList.lastOption.getOrElse("")
    List("main", "master").contains(branch)
  }
  private def isMainBranchCommand3: Boolean = {
    // starting from git 2.2
    val branch =
      Process("git branch --show-current",repoPath).lineStream_!.headOption.getOrElse("")
    List("main", "master").contains(branch)
  }
  private def versionTypeByComment: Option[VersionType] = comment match {
    case c if c.contains("[main]") => Some(VersionType.MAIN)
    case c if c.contains("[snapshot]") => Some(VersionType.SNAPSHOT)
    case _ => None
  }

  private lazy val hash:String = Process("git rev-parse --short HEAD",repoPath).lineStream_!.toList.headOption.getOrElse("")
}
