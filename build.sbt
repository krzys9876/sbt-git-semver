name := "git-semver-mmp"
ThisBuild / sbtPlugin := true
ThisBuild / version := "1.1.0"
ThisBuild / versionScheme := Some("early-semver")

scalaVersion := "2.12.17" // consistent with version used by sbt 1.8.2
libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "3.2.15" % Test)

ThisBuild / organization := "io.github.krzys9876"
ThisBuild / organizationName := "krzys9876"
ThisBuild / organizationHomepage := Some(url("https://github.com/krzys9876"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/krzys9876/sbt-git-semver"),
    "scm:git@github.com:krzys9876/sbt-git-semver.git"))
ThisBuild / developers := List(
  Developer(
    id = "krzys9876",
    name = "Krzysztof Ruta",
    email = "krzys9876@gmail.com",
    url = url("https://github.com/krzys9876")))
ThisBuild / description := "Sbt plugin for automatic version numbering using git tags and major.minor.patch scheme."
ThisBuild / licenses := List("MIT" -> new URL("https://opensource.org/license/mit/"))
ThisBuild / homepage := Some(url("https://github.com/krzys9876/sbt-git-semver"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
