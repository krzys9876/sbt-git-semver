import scala.sys.process.Process

/**
 * Implements set of operations on git repository, passes the git parameters to GitVersions instance, which implements
 * the actual versioning logic
 */
case class GitHandler() {
  lazy val versions:GitVersions = GitVersions(tags,describedTag,comment,isMainBranch,hash)
  def pushTagIfChanged(): Unit =
    if(!versions.isSame) {
    Process(f"git tag ${versions.next.toString}").!
    Process("git push --tags").!
  }

  private lazy val comment: String = Process("git log -1 --pretty=%B").lineStream_!.toList.mkString(" ")
  private lazy val tags: List[String] = {
    Process("git pull --tags")
    Process("git tag").lineStream_!.toList
  }
  private lazy val describedTag: String = Process("git describe --tags").lineStream_!.toList.headOption.getOrElse("")
  private lazy val isMainBranch:Boolean = {
    val branch = Process("git symbolic-ref --short HEAD").lineStream_!.headOption.getOrElse("")
    List("main","master").contains(branch)
  }
  private lazy val hash:String = Process("git rev-parse --short HEAD").lineStream_!.toList.headOption.getOrElse("")
}
