/**
 * The class implements the actual logic of finding the new version depending on git repo parameters
 *
 * @param tags A list of tags in git repository
 *
 * @param describedTag The tag for the current commit. It is used to check if the last commit is already tagged
 *                     with the new version
 *
 * @param gitComment A comment to the current commit, used to find [next_major] or [next_minor] tokens that
 *                   override default behaviour to calculate next patch
 *
 * @param versionType MAIN if the current branch the main branch or SNAPSHOT some other branch. Semantic versioning
 *                     with the major.minor.patch patterns is applied only to the main version type,
 *                     snapshot version gets a suffix with the commit hash followed by SNAPSHOT
 *
 * @param hash Current commit's hash, used for adding suffix to the version for non-main branch
 */
case class GitVersions(tags:List[String],describedTag:String,gitComment:String,versionType:VersionType,hash:String) {
  val versions:List[GitVersion] = tags.flatMap(GitVersion.parse).sortWith(GitVersion.lt).reverse
  val current:GitVersion = versions.headOption.getOrElse(GitVersion.ZERO)

  private val described:Option[GitVersion] = GitVersion.parse(describedTag)
  private val bumpMajor: Boolean = gitComment.toLowerCase.contains("[next_major]")
  private val bumpMinor: Boolean = gitComment.toLowerCase.contains("[next_minor]")
  private val bumped:GitVersion = (bumpMajor,bumpMinor) match {
    case(true,_) => current.nextMajor
    case(false,true) => current.nextMinor
    // do not bump first version
    case _ => if(current == GitVersion.ZERO && versions.isEmpty) GitVersion.ZERO else current.nextPatch
  }

  val next: GitVersion = (described, versionType) match {
    case (Some(descVer), _) => descVer
    case (None, VersionType.MAIN) => bumped
    case (None, VersionType.SNAPSHOT) => bumped.withSuffix(f"-$hash-SNAPSHOT")
  }
  val isSame: Boolean = described.contains(next) || describedTag == next.toString
}

object GitVersions {
  def apply(tags:List[String]):GitVersions = new GitVersions(tags,"","",VersionType.MAIN,"")
}

case class GitVersion(major:Int, minor:Int, patch:Int,suffix:String="") {
  lazy val nextPatch:GitVersion = GitVersion(major, minor, patch+1,suffix)
  lazy val nextMinor:GitVersion = GitVersion(major, minor+1, 0,suffix)
  lazy val nextMajor:GitVersion = GitVersion(major+1, 0, 0,suffix)
  def withSuffix(newSuffix:String): GitVersion = copy(suffix=newSuffix)
  override def toString: String = f"$major.$minor.$patch$suffix"
}

/**
 * A value object for a version matching semantic versioning principles (using major.minor.patch pattern)
 * Implements 'next' and sorting logics
 */
object GitVersion {
  private val pattern:String = "^\\d+.\\d+.\\d$"
  def parse(tag:String):Option[GitVersion] = if(tag.matches(pattern))
    tag.split('.') match {
      case Array(major,minor,patch) => Some(GitVersion(major.toInt,minor.toInt,patch.toInt))
      case _ => None
    } else None
  def lt(v1:GitVersion,v2:GitVersion):Boolean = (v1,v2) match {
    case (GitVersion(ma1,mi1,pa1,_),GitVersion(ma2,mi2,pa2,_)) if ma1==ma2 && mi1==mi2 => pa1<pa2
    case (GitVersion(ma1,mi1,_,_),GitVersion(ma2,mi2,_,_)) if ma1==ma2 => mi1<mi2
    case (GitVersion(ma1,_,_,_),GitVersion(ma2,_,_,_)) => ma1<ma2
  }
  val ZERO:GitVersion = new GitVersion(0,0,0,"")
}

sealed trait VersionType

object VersionType {
  case object MAIN extends VersionType
  case object SNAPSHOT extends VersionType
}