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
    Process("git fetch --tags",repoPath).!
    Process("git tag",repoPath).lineStream_!.toList
  }
  private lazy val describedTag: String = Process("git describe --tags",repoPath).lineStream_!.toList.headOption.getOrElse("")
  private lazy val versionType:VersionType =
    overrideVersionType.getOrElse(
      versionTypeByComment.getOrElse(
        if(isMainBranch) VersionType.MAIN else VersionType.SNAPSHOT))

  private def isMainBranch:Boolean = branchInfo.map(_.getOrElse("")).exists(List("main","master").contains)
  private def branchInfo:List[Option[String]] = List(
    // from some thread on StackOverflow
    Process("git symbolic-ref --short HEAD",repoPath).lineStream_!.headOption,
    // in Jenkins (observed in one production environment, git 1.8) it returns: remotes/origin/master
    Process("git name-rev --name-only HEAD",repoPath).lineStream_!.headOption.getOrElse("").split('/').toList.lastOption,
    // starting from git 2.2
    Process("git branch --show-current",repoPath).lineStream_!.headOption,
    // Gitlab CI predefined variable
    sys.env.get("CI_COMMIT_BRANCH")
  )

  private def versionTypeByComment: Option[VersionType] = comment match {
    case c if c.contains("[main]") => Some(VersionType.MAIN)
    case c if c.contains("[snapshot]") => Some(VersionType.SNAPSHOT)
    case _ => None
  }

  private lazy val hash:String = Process("git rev-parse --short HEAD",repoPath).lineStream_!.toList.headOption.getOrElse("")
}
