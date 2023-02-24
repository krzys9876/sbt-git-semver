import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

class GitVersionsTest extends AnyFeatureSpec with GivenWhenThen {
  Feature("decode version from git tags using semantic versioning pattern of: major.minor.patch") {
    Scenario("read and decode list of correct tags") {
      Given("unsorted list of correct tags")
      val tags=List("0.0.1","0.0.2","0.1.2","1.11.15","12.10.15","1.1.3")
      When("parsed")
      val versions=GitVersions(tags)
      Then("all tags are decoded and sorted")
      val expected=List(GitVersion(12,10,15),GitVersion(1,11,15),GitVersion(1,1,3),GitVersion(0,1,2),GitVersion(0,0,2),GitVersion(0,0,1))
      assert(versions.versions == expected)
      assert(versions.current == GitVersion(12,10,15))
    }
    Scenario("read and decode list of correct tags (large numbers)") {
      Given("unsorted list of correct tags")
      val tags = List("0.0.10001", "0.0.10002", "0.99999.2", "1234567890.11.1500", "1234567891.10.15", "1999999.1.3")
      When("parsed")
      val versions = GitVersions(tags)
      Then("all tags are decoded and sorted")
      val expected = List(GitVersion(1234567891, 10, 15), GitVersion(1234567890, 11, 1500), GitVersion(1999999, 1, 3),
        GitVersion(0, 99999, 2), GitVersion(0, 0, 10002), GitVersion(0, 0, 10001))
      assert(versions.versions == expected)
      assert(versions.current == GitVersion(1234567891, 10, 15))
    }
    Scenario("read and decode list of incorrect tags") {
      Given("unsorted list containing incorrect tags")
      val tags = List("0.0.1", "0.A.2", "0.1.2", "1.B5", "1.1.3")
      When("parsed")
      val versions = GitVersions(tags)
      Then("all correct tags are decoded and incorrect tags are ignored")
      val expected = List(GitVersion(1, 1, 3), GitVersion(0, 1, 2), GitVersion(0, 0, 1))
      assert(versions.versions == expected)
      assert(versions.current == GitVersion(1,1,3))
    }
    Scenario("read and decode list of only incorrect tags") {
      Given("unsorted list containing only incorrect tags")
      val tags = List("X.0.1", "0.A.2", "0.1.Y", "1.B.5", "1.Z.3")
      When("parsed")
      val versions = GitVersions(tags)
      Then("resulting list is empty and current version is 0.0.0")
      assert(versions.versions.isEmpty)
      assert(versions.current == GitVersion(0,0,0))
    }
    Scenario("read and decode empty list") {
      Given("empty list")
      val tags = List[String]()
      When("parsed")
      val versions = GitVersions(tags)
      Then("resulting list is empty and current version is 0.0.0")
      assert(versions.versions.isEmpty)
      assert(versions.current == GitVersion(0, 0, 0))
    }
  }
  Feature("bump version by selected segment") {
    Scenario("bump patch") {
      Given("list of versions")
      val initial=List(GitVersion(0,0,0),GitVersion(2,1,5),GitVersion(5,2,12))
      When("next patch is requested")
      val nextPatch=initial.map(_.nextPatch)
      Then("patch is increased by 1")
      assert(nextPatch == List(GitVersion(0,0,1),GitVersion(2,1,6),GitVersion(5,2,13)))
    }
    Scenario("bump minor") {
      Given("list of versions")
      val initial = List(GitVersion(0, 0, 0), GitVersion(2, 1, 5), GitVersion(5, 2, 12))
      When("next minor is requested")
      val nextPatch = initial.map(_.nextMinor)
      Then("minor is increased by 1 and patch is set to 0")
      assert(nextPatch == List(GitVersion(0, 1, 0), GitVersion(2, 2, 0), GitVersion(5, 3, 0)))
    }
    Scenario("bump major") {
      Given("list of versions")
      val initial = List(GitVersion(0, 0, 0), GitVersion(2, 1, 5), GitVersion(5, 2, 12))
      When("next major is requested")
      val nextPatch = initial.map(_.nextMajor)
      Then("major is increased by 1 and minor and patch are set to 0")
      assert(nextPatch == List(GitVersion(1, 0, 0), GitVersion(3, 0, 0), GitVersion(6, 0, 0)))
    }
  }

  Feature("determine next version from git repository") {
    Scenario("untagged commit on main branch") {
      Given("git repo with untagged commit on main branch and existing tags")
      val tags=List("0.0.1","0.0.2","2.1.2","1.1.6","1.1.3")
      val versions=GitVersions(tags,"none","some comment",VersionType.MAIN,"abcd")
      When("next version is determined")
      val next=versions.next
      Then("it is the next patch for current version")
      assert(next==GitVersion(2,1,3))
    }
    Scenario("properly tagged commit on main branch") {
      Given("git repo with properly tagged commit on main branch and existing tags")
      val tags = List("0.0.1", "0.0.2", "2.1.2", "1.1.6", "1.1.3")
      val versions = GitVersions(tags, "2.1.2", "some comment", VersionType.MAIN, "abcd")
      When("next version is determined")
      val next = versions.next
      Then("it is the current version")
      assert(next == GitVersion(2, 1, 2))
      assert(versions.isSame)
    }
    Scenario("commit tagged with earlier number on main branch") {
      Given("git repo with properly tagged commit on main branch and existing tags")
      val tags = List("0.0.1", "0.0.2", "2.1.2", "1.1.6", "1.1.3")
      val versions = GitVersions(tags, "1.1.6", "some comment", VersionType.MAIN, "abcd")
      When("next version is determined")
      val next = versions.next
      Then("it is the current version even if it is earlier number")
      assert(next == GitVersion(1, 1, 6))
    }
    Scenario("untagged commit on other branch") {
      Given("git repo with properly tagged commit on other branch and existing tags")
      val tags = List("0.0.1", "0.0.2", "2.1.2", "1.1.6", "1.1.3")
      val versions = GitVersions(tags, "none", "some comment", VersionType.SNAPSHOT, "QWERTY")
      When("next version is determined")
      val next = versions.next
      Then("it is the next patch for current version with hash and suffix")
      assert(next == GitVersion(2, 1, 3, "-QWERTY-SNAPSHOT"))
    }
    Scenario("properly tagged commit on other branch (no commit since last plugin run)") {
      Given("git repo with properly tagged commit on other branch and existing tags")
      val tags = List("0.0.1", "0.0.2", "2.1.2", "1.1.6", "1.1.3")
      val versions = GitVersions(tags, "2.1.3-abcd-SNAPSHOT", "some comment", VersionType.SNAPSHOT, "abcd")
      When("next version is determined")
      val next = versions.next
      Then("it is the current version")
      assert(next == GitVersion(2, 1, 3, "-abcd-SNAPSHOT"))
      assert(versions.isSame)
    }
    Scenario("untagged commit on main branch with [next_major] in commit comment") {
      Given("git repo with untagged commit on main branch and existing tags and with [next_major] in comment")
      val tags = List("0.0.1", "0.0.2", "2.1.2", "1.1.6", "1.1.3")
      val versions = GitVersions(tags, "none", "some [next_major] comment", VersionType.MAIN, "abcd")
      When("next version is determined")
      val next = versions.next
      Then("it is the next major for current version")
      assert(next == GitVersion(3, 0, 0))
    }
    Scenario("untagged commit on main branch with [next_minor] in commit comment") {
      Given("git repo with untagged commit on main branch and existing tags and with [next_minor] in comment")
      val tags = List("0.0.1", "0.0.2", "2.1.2", "1.1.6", "1.1.3")
      val versions = GitVersions(tags, "none", "some [next_minor] comment", VersionType.MAIN, "abcd")
      When("next version is determined")
      val next = versions.next
      Then("it is the next minor for current version")
      assert(next == GitVersion(2, 2, 0))
    }
    Scenario("untagged commit on other branch with [next_major] in commit comment") {
      Given("git repo with properly tagged commit on other branch and existing tags and with [next_major] in comment")
      val tags = List("0.0.1", "0.0.2", "2.1.2", "1.1.6", "1.1.3")
      val versions = GitVersions(tags, "none", "some [next_major] comment", VersionType.SNAPSHOT, "QWERTY")
      When("next version is determined")
      val next = versions.next
      Then("it is the next patch for current version with hash and suffix")
      assert(next == GitVersion(3, 0, 0, "-QWERTY-SNAPSHOT"))
    }
    Scenario("untagged commit on other branch with [next_minor] in commit comment") {
      Given("git repo with properly tagged commit on other branch and existing tags and with [next_minor] in comment")
      val tags = List("0.0.1", "0.0.2", "2.1.2", "1.1.6", "1.1.3")
      val versions = GitVersions(tags, "none", "some [next_minor] comment", VersionType.SNAPSHOT, "QWERTY")
      When("next version is determined")
      val next = versions.next
      Then("it is the next patch for current version with hash and suffix")
      assert(next == GitVersion(2, 2, 0, "-QWERTY-SNAPSHOT"))
    }
    Scenario("no tags on main branch") {
      Given("git repo w/o tags on main branch")
      val tags = List()
      val versions = GitVersions(tags, "none", "some comment", VersionType.MAIN, "QWERTY")
      When("next version is determined")
      val next = versions.next
      Then("it is 0.0.0")
      assert(next == GitVersion(0, 0, 0))
    }
    Scenario("no tags on other branch") {
      Given("git repo w/o tags on main branch")
      val tags = List()
      val versions = GitVersions(tags, "none", "some comment", VersionType.SNAPSHOT, "QWERTY")
      When("next version is determined")
      val next = versions.next
      Then("it is 0.0.0 with hash and suffix")
      assert(next == GitVersion(0, 0, 0, "-QWERTY-SNAPSHOT"))
    }
  }
}
