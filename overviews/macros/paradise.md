---
layout: overview-large
title: Macro Paradise

disqus: true

partof: macros
num: 2
outof: 4
---
<span class="label warning" style="float: right;">MACRO PARADISE</span>

**Eugene Burmako**

Macro paradise is an alias of the experimental `paradise/macros` branch in the official Scala repository, designed to facilitate rapid development of macros without compromising the stability of Scala. To learn more about this branch, check out [my talk](http://scalamacros.org/news/2012/12/18/macro-paradise.html).

We have set up a nightly build which publishes snapshot artifacts to Sonatype. Consult [https://github.com/scalamacros/sbt-example-paradise](https://github.com/scalamacros/sbt-example-paradise) for an end-to-end example of using our nightlies in SBT, but in a nutshell playing with macro paradise is as easy as adding these three lines to your build:

    scalaVersion := "2.11.0-SNAPSHOT"
    scalaOrganization := "org.scala-lang.macro-paradise"
    resolvers += Resolver.sonatypeRepo("snapshots")
