case class GitVersions(tags:List[String],describedTag:String,gitComment:String,isMainBranch:Boolean,hash:String) {
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

  val next: GitVersion = (described, isMainBranch) match {
    case (Some(descVer), _) => descVer
    case (None, true) => bumped
    case (None, false) => bumped.withSuffix((f"-$hash-SNAPSHOT"))
  }
  val isSame: Boolean = described.contains(next) || describedTag == next.toString
}

object GitVersions {
  def apply(tags:List[String]):GitVersions = new GitVersions(tags,"","",true,"")
}

case class GitVersion(major:Int, minor:Int, patch:Int,suffix:String="") {
  lazy val nextPatch:GitVersion = GitVersion(major, minor, patch+1,suffix)
  lazy val nextMinor:GitVersion = GitVersion(major, minor+1, 0,suffix)
  lazy val nextMajor:GitVersion = GitVersion(major+1, 0, 0,suffix)
  def withSuffix(newSuffix:String): GitVersion = copy(suffix=newSuffix)
  override def toString: String = f"$major.$minor.$patch$suffix"
}

object GitVersion {
  val pattern:String = "^\\d+.\\d+.\\d$"
  def parse(tag:String):Option[GitVersion] = if(tag.matches(pattern))
    tag.split('.') match {
      case Array(major,minor,patch) => Some(GitVersion(major.toInt,minor.toInt,patch.toInt))
      case _ => None
    } else None
  def lt(v1:GitVersion,v2:GitVersion):Boolean = (v1,v2) match {
    case (GitVersion(ma1,mi1,pa1,_),GitVersion(ma2,mi2,pa2,_)) if ma1==ma2 && mi1==mi2 => pa1<pa2
    case (GitVersion(ma1,mi1,pa1,_),GitVersion(ma2,mi2,pa2,_)) if ma1==ma2 => mi1<mi2
    case (GitVersion(ma1,mi1,pa1,_),GitVersion(ma2,mi2,pa2,_)) => ma1<ma2
  }
  val ZERO:GitVersion = new GitVersion(0,0,0,"")
}