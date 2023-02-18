version := "1.0.0"
sbtPlugin := true
organization := "org.kr.sbt"
name := "git-semver-mmp"
scalaVersion := "2.12.17" // consistent with version used by sbt 1.8.2

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
)
