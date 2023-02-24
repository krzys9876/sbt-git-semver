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

It performs the following steps:

1. checks all tags matching the m.m.p pattern
2. checks if the last commit is tagged, i.e. was the next version already determined and pushed
3. calculates next version

The next version is the next patch by default. You can add _[next_major]_  or  _[next_minor]_
to the commit message to bump major or minor version respectively.

On main/master branch this is all, on other branches it adds a suffix: hash followed by _SNAPSHOT_.
Sbt recognizes a snapshot version based on it and allows publishing it to different repository than a proper release.
I don't use it anyway ;) but it might be useful when using development branches.

The plugin provides 3 commands: 

    sbt "gitSemverNextVersion version.txt"
    sbt "gitSemverNextVersionMain version.txt"
    sbt "gitSemverNextVersionSnapshot version.txt"

The first follows rules described above. The other two override these rules. You may also wish to override version type by including 
_[main]_ or _[snapshot]_ respectively in commit message.

The file name, where the new version number is stored, is optional. 
I need this in my CI/CD scripts to save version number to other audit repo.

The plugin is published to Central, so if you want to use it, add it to your project plugins in <code>project/plugins.sbt</code>:

    addSbtPlugin("io.github.krzys9876" % "git-semver-mmp" % "1.1.1")

If you want to play with the code, it is sufficient that your <code>build.sbt</code> includes just the following entries:

    version := "0.0.1"
    sbtPlugin := true // NOTE: this is necessary for plugin to compile!
    organization := "some.organization"
    name := "some-name"
    scalaVersion := "2.12.17"

## Side note on ChatGPT

I started to use CharGPT recently, at first out of curiosity, than as a supplement to web search and Stack Overflow.
I asked it to write me a plugin that uses git tags in a m.m.p format to handle versioning. It was correct, precise and 
very condensed (too condensed to me - a single object with all logics), but very useful to show me the concept of sbt plugins
and git integration. I've read some other tutorials which helped me actually build the plugin 
(e.g. <code>sbtPlugin:=true</code>). I did not specify all my requirements, because after initial exploration of results 
I just switched to the code.

As many of its users, I find ChatGPT to be an excellent tool for code snippets, application skeleton, 
but obviously not for production code. I definitely prefer a TDD approach and evolving design instead of 
using a large portions of generated code to fiddle with. As a starting point and a tool to find explanations
it's brilliant, so I'm sure I will be using it as one of handy tools to boost my productivity. 
What I find difficult in working with ChatGPT is the necessity to specify exactly what I need in plain text,
which sometimes takes more time that actually writing the code. Which leads me to a conclusion that our work as 
software developers is not threatened by it, but quite conversely that as an industry we've just got
a new great tool. We should just should use it wisely.
