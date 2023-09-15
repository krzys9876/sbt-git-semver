import org.scalatest.{BeforeAndAfterAll, GivenWhenThen}
import org.scalatest.featurespec.AnyFeatureSpec

import java.io.File
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import scala.sys.process.Process

class GitVersionsIntegrationTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll {
  override def beforeAll(): Unit = {
    if (Files.exists(Paths.get(workingDir))) deleteFiles(Paths.get(workingDir).toFile)
    Files.createDirectory(Paths.get(workingDir))
  }

  lazy val workingDir: String =
    new java.io.File("../_git_semver_tmp_files").getCanonicalPath.replace("\\", "/")
  def fileWithPath(name : String) : Path = Paths.get(workingDir, name)
  private def deleteFiles(file: File): Unit = {
    if (file.isDirectory) file.listFiles.foreach(deleteFiles)
    if (file.exists) file.delete
  }
  def fileExists(name: String): Boolean = Files.exists(fileWithPath(name))
  private val versionFile:String="version.txt"

  Feature("get next version for empty git repository") {
    Scenario("first version for empty repo before first commit") {
      Given("empty git repository")
      val repoDir=createRepo("t1")
      When("version is calculated on main branch")
      val version=GitHandler(None,Some(repoDir.toFile)).nextVersion
      Then("version is 0.0.0")
      assert(version==GitVersion.ZERO)
    }
    Scenario("first version for empty repo after first commit") {
      Given("a repository with 1 commit to main")
      val repoDir = createRepoWithSingleCommitToMain("t2")
      When("version is calculated on main branch")
      val version = GitHandler(None, Some(repoDir.toFile)).nextVersion
      Then("version is 0.0.0")
      assert(version == GitVersion.ZERO)
    }
    Scenario("first version for empty repo before first commit to branch (snapshot)") {
      Given("a repository with 1 commit to main and an empty branch")
      val repoDir = createRepoWithSingleCommitToMain("t3")
      Process("git checkout -b some_branch",repoDir.toFile).!
      When("version is calculated on other branch")
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)),Some(versionFile),None,doPush = false)
      Then("version is 0.0.0")
      assert(readVersionFile(repoDir) == "0.0.0")
    }
    Scenario("first version for empty repo after first commit to branch (snapshot)") {
      Given("a repository with 1 commit to main, 1 commit to branch")
      val repoDir = createRepoWithCommitsToMainAndBranch("t4","some_branch")
      When("version is calculated on other branch")
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      Then("version is 0.0.0")
      assertSnapshot("0.0.0",readVersionFile(repoDir))
    }
  }	

  Feature("consecutive commits to main") {
    Scenario("next patch, minor and major on main") {
      Given("a repository with single commit to main and initial version of 0.0.0")
      val repoDir = createRepoWithSingleCommitToMain("t5")
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      When("a commit is made (w/o next_minor or next_major")
      Process("git commit -a -m 'second_commit' --allow-empty", repoDir.toFile).!
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      Then("next version is 0.0.1")
      assert(readVersionFile(repoDir) == "0.0.1")
      When("a commit is made with [next_minor]")
      Process("git commit -a -m 'bump_minor_[next_minor]_commit' --allow-empty", repoDir.toFile).!
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      Then("next version is 0.1.0")
      assert(readVersionFile(repoDir) == "0.1.0")
      When("a commit is made (w/o next_minor or next_major")
      Process("git commit -a -m 'bump_patch_commit' --allow-empty", repoDir.toFile).!
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      Then("next version is 0.1.1")
      assert(readVersionFile(repoDir) == "0.1.1")
      When("a commit is made with [next_major]")
      Process("git commit -a -m 'bump_major_[next_major]_commit' --allow-empty", repoDir.toFile).!
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      Then("next version is 1.0.0")
      assert(readVersionFile(repoDir) == "1.0.0")
      When("a commit is made (w/o next_minor or next_major")
      Process("git commit -a -m 'bump_patch_commit' --allow-empty", repoDir.toFile).!
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      Then("next version is 1.0.1")
      assert(readVersionFile(repoDir) == "1.0.1")
    }
    Scenario("bump version without a commit on main") {
      Given("a repository with initial version of 0.1.0")
      val repoDir = createRepoWithVersion001("t6")
      Process("git commit -a -m 'bump_minor_[next_minor]_commit' --allow-empty", repoDir.toFile).!
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      assert(readVersionFile(repoDir) == "0.1.0")
      When("a sbt command is executed several times with different options (w/o any commit)")
      Then("the version remains unchanged")
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      assert(readVersionFile(repoDir) == "0.1.0")
      GitSemverPlugin.handleCommand(GitHandler(Some(VersionType.MAIN), Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      assert(readVersionFile(repoDir) == "0.1.0")
      GitSemverPlugin.handleCommand(GitHandler(Some(VersionType.SNAPSHOT), Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      assert(readVersionFile(repoDir) == "0.1.0")
    }
    Scenario("bump snapshot version on main (by command)") {
      Given("a repository with initial version of 0.0.1")
      val repoDir = createRepoWithVersion001("t7")
      When("a command is executed with snapshot parameter")
      Process("git commit -a -m 'another_commit' --allow-empty", repoDir.toFile).!
      GitSemverPlugin.handleCommand(GitHandler(Some(VersionType.SNAPSHOT), Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      Then("next version is a snapshot of 0.0.2")
      assertSnapshot("0.0.2",readVersionFile(repoDir))
    }
    Scenario("bump snapshot version on main (by commit message)") {
      Given("a repository with initial version of 0.0.1")
      val repoDir = createRepoWithVersion001("t8")
      When("a commit i made with [snapshot] parameter")
      Process("git commit -a -m 'a_forced_[snapshot]_commit' --allow-empty", repoDir.toFile).!
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      Then("next version is a snapshot of 0.0.2")
      assertSnapshot("0.0.2", readVersionFile(repoDir))
    }
    Scenario("bump main version on main (by command)") {
      Given("a repository with initial version of 0.0.1")
      val repoDir = createRepoWithVersion001("t9")
      When("a command is executed with main parameter")
      Process("git commit -a -m 'another_commit' --allow-empty", repoDir.toFile).!
      GitSemverPlugin.handleCommand(GitHandler(Some(VersionType.MAIN), Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      Then("next version is 0.0.2 (no difference)")
      assert(readVersionFile(repoDir)=="0.0.2")
    }
    Scenario("bump main version on main (by commit message)") {
      Given("a repository with initial version of 0.0.1")
      val repoDir = createRepoWithVersion001("t10")
      When("a commit is made with [main] parameter")
      Process("git commit -a -m 'a_forced_[main]_commit' --allow-empty", repoDir.toFile).!
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      Then("next version is 0.0.2 (no difference)")
      assert(readVersionFile(repoDir)=="0.0.2")
    }
    Scenario("bump snapshot version and minor on main (combine message flags)") {
      Given("a repository with initial version of 0.0.1")
      val repoDir = createRepoWithVersion001("t11")
      When("a commit is made with [snapshot] parameter")
      Process("git commit -a -m 'a_forced_[snapshot][next_minor]_commit' --allow-empty", repoDir.toFile).!
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      Then("next version is a snapshot of 0.1.0")
      assertSnapshot("0.1.0", readVersionFile(repoDir))
    }
  }
  Feature("commits to branch") {
    Scenario("next minor on branch") {
      Given("a repository with initial version of 0.0.1 and on branch")
      val repoDir = createRepoWithVersion001("t12")
      Process("git checkout -b some_other_branch", repoDir.toFile).!
      When("a commit is made with [next_minor] parameter")
      Process("git commit -a -m 'a_[next_minor]_commit' --allow-empty", repoDir.toFile).!
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      Then("next version is a snapshot of 0.1.0")
      assertSnapshot("0.1.0", readVersionFile(repoDir))
    }
    Scenario("next major on branch") {
      Given("a repository with initial version of 0.0.1 and on branch")
      val repoDir = createRepoWithVersion001("t13")
      Process("git checkout -b some_other_branch", repoDir.toFile).!
      When("a commit is made with [next_major] parameter")
      Process("git commit -a -m 'a_[next_major]_commit' --allow-empty", repoDir.toFile).!
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      Then("next version is a snapshot of 1.0.0")
      assertSnapshot("1.0.0", readVersionFile(repoDir))
    }
    Scenario("bump version without a commit on branch") {
      Given("a repository with initial version of 0.0.1 and on branch")
      val repoDir = createRepoWithVersion001("t14")
      Process("git checkout -b some_other_branch", repoDir.toFile).!
      Process("git commit -a -m 'bump_minor_[next_minor]_commit' --allow-empty", repoDir.toFile).!
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      assertSnapshot("0.1.0",readVersionFile(repoDir))
      assertSnapshot("0.1.0",readVersionFile(repoDir))
      When("a sbt command is executed several times with snapshot option or with default (w/o any commit)")
      Then("the version remains unchanged")
      GitSemverPlugin.handleCommand(GitHandler(None, Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      assertSnapshot("0.1.0",readVersionFile(repoDir))
      GitSemverPlugin.handleCommand(GitHandler(Some(VersionType.SNAPSHOT), Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      assertSnapshot("0.1.0",readVersionFile(repoDir))
      When("a sbt command is executed with main version option (w/o any commit)")
      GitSemverPlugin.handleCommand(GitHandler(Some(VersionType.MAIN), Some(repoDir.toFile)), Some(versionFile), None, doPush = false)
      Then("the version is changed to main (as main version has a priority over snapshot)")
      assert(readVersionFile(repoDir)=="0.1.0")
    }
  }

  private def createRepo(dir:String):Path = {
    val gitRepoDir=fileWithPath(dir)
    Files.createDirectory(gitRepoDir)
    Process("git init",gitRepoDir.toFile).!
    gitRepoDir
  }

  private def createRepoWithSingleCommitToMain(dir: String): Path = {
    val gitRepoDir = createRepo(dir)
    createFile(gitRepoDir)
    Process("git add -A", gitRepoDir.toFile).!
    Process("git commit -a -m 'initial'", gitRepoDir.toFile).!
    gitRepoDir
  }

  private def createRepoWithCommitsToMainAndBranch(dir: String,branch:String): Path = {
    val gitRepoDir = createRepo(dir)
    createFile(gitRepoDir)
    Process("git add -A", gitRepoDir.toFile).!
    Process("git commit -a -m 'initial'", gitRepoDir.toFile).!
    Process("git checkout -b some_branch", gitRepoDir.toFile).!
    createFile(gitRepoDir, "some_other_file.txt")
    Process("git add some_other_file.txt", gitRepoDir.toFile).!
    //NOTE: spaces in message replaced with unserscores as this caused errors (as if Process ignored single and double quotes)
    Process("git commit some_other_file.txt -m 'new_file_on_branch'", gitRepoDir.toFile).!
    gitRepoDir
  }

  private def createRepoWithVersion001(dir: String): Path = {
    val gitRepoDir = createRepo(dir)
    createFile(gitRepoDir)
    Process("git add -A", gitRepoDir.toFile).!
    Process("git commit -a -m 'initial'", gitRepoDir.toFile).!
    GitSemverPlugin.handleCommand(GitHandler(None, Some(gitRepoDir.toFile)), Some(versionFile), None, doPush = false)
    Process("git commit -a -m 'empty_commit' --allow-empty", gitRepoDir.toFile).!
    GitSemverPlugin.handleCommand(GitHandler(None, Some(gitRepoDir.toFile)), Some(versionFile), None, doPush = false)
    gitRepoDir
  }


    private def createFile(path:Path,name:String="some_file.txt",content:String="123"):Unit =
    Files.write(Paths.get(path.toFile.getAbsolutePath, name), content.toCharArray.map(_.toByte), StandardOpenOption.CREATE_NEW)

  private def readVersionFile(path:Path):String = Files.readString(Paths.get(path.toFile.getAbsolutePath,versionFile))
  
  private def assertSnapshot(expected:String,version:String):Unit = {
    assert(version.startsWith(expected) && version.endsWith("-SNAPSHOT"))

  }
}

