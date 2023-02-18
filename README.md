# Sbt plugin for automatic version numbering #

This is just another plugin for automatic version numbering using semantic versioning rules. 
I decided to trim sem-ver just to major.minor.patch pattern as this is what I use in day to day work.

## Background ##

One day I joined a project where all CI/CD processed (Jenkins based) were using python plugin for versioning. 
It worked fine, but I felt that it is not quite right to use python with scala all over the place for such a trivial functionality.

Moreover - using sbt plugin the pipeline could be simplified as it would not contain the boilerplate related to python venvs.

## Principles ##

I'm a trunk-based development fanboy and I hardly use branches. 
I don't say they are useless, but I would not benefit from branches in my current work at all. 
So I am interested in simple workflow of:

* commit to main

* run commit stage of CI/CD with automatic tests, package and publish artifacts

* run other tasks in production-like environment

Since I use Scala all the time, practically all commit-stage of CI/CD scripts could be run 
using sbt - if there was a suitable plugin for versioning. I reviewed some existing ones, 
but they mostly felt a bit overcomplicated or just not too verbose at first sight. 
So I decided to write my own and learn some new stuff.

## How to use ##

THe plugin assumes that you are in a git repository and that a default remote is defined. 
Note that currently all errors related to git are suppressed, but you will see them in the sbt log.

There is only one command: 

    sbt "gitSemverNextVersion version.txt"

where file name, where the new version number is stored, is optional. 
I need this in my CI/CD scripts to save version number to other audit repo.

This is all it does:

1. checks all tags matching the m.m.p pattern
2. checks if the last commit is tagged, i.e. was the next version already determined and pushed
3. calculates next version

The next version is a next patch by default. You can add _[next_major]_  or  _[next_minor]_ 
to the commit message to bump major or minor version respectively.

On main/master branch this is all, on other branches it adds a suffix: hash followed by _SNAPSHOT_ (just to follow the convention used by my colleagues). 
Sbt recognizes a snapshot version based on it and allows publishing it to different repository than a proper release. 
I don't use it anyway :)

To enable the plugin in your own sbt project, all you need is to add the following line to your _project/plugins.sbt_ file:

    addSbtPlugin("org.kr.sbt" % "git-semver-mmp" % "1.0.0")

Make sure to update the organization, plugin name and version, so they match the sbt file. In my case:

    version := "1.0.0"
    organization := "org.kr.sbt"
    name := "git-semver-mmp"

Bit before, if you want to use it locally, publish the plugin to your local Ivy repo:

    sbt publishLocal