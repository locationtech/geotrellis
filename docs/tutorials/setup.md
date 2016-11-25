Welcome to GeoTrellis, the [Scala](http://www.scala-lang.org/) library for
high-performance geographic data processing. Being a library, users import
GeoTrellis and write their own Scala applications with it. This guide will
help you get up and running with a basic GeoTrellis development environment.

Requirements
------------

[Java 8](http://www.oracle.com/technetwork/java/javase/overview/index.html).
GeoTrellis code won't function with Java 7 or below. You can test your Java
version by entering the following in a Linux or Mac terminal:

```console
> javac -version
javac 1.8.0_102
```

You want to see `1.8` like above.

Using Scala
-----------

GeoTrellis is a Scala library, so naturally you must write your applications
in Scala. If you're new to Scala, we recommend the following:

- The [official Scala tutorials](http://www.scala-lang.org/documentation/)
- The [Scala Cookbook](http://shop.oreilly.com/product/0636920026914.do) as
a handy language reference
- [99 Problems in Scala](http://aperiodic.net/phil/scala/s-99/) to develop basic skills
in Functional Programming

`sbt` - The Simple Build Tool
-----------------------------

Did you know you don't even need to install Scala yourself to develop with
it? `sbt` allows us to manage Scala projects, their dependencies, and even
Scala compiler versions.

To install on a Mac with [Homebrew](http://brew.sh/):

```console
> brew install sbt
```

For Linux users, `sbt` should be available in your repositories.

As of 2016 October 24, the latest version of `sbt` is `0.13.12`. The rest of
the guide assumes you're using this version.

Simple Project Template
-----------------------

The simplest sbt-based GeoTrellis project needs only two things:

In `your-projecet/project/build.properies`:

```bash
sbt.version=0.13.12
```

and in `your-project/build.sbt`:

```scala
name := """your-project"""

version := "1.0.0"

scalaVersion := "2.11.8"

/* Comment out whichever geotrellis-* modules you don't need */
libraryDependencies ++= Seq(
    "com.azavea.geotrellis" %% "geotrellis-spark"  % "0.10.3",
    "com.azavea.geotrellis" %% "geotrellis-raster" % "0.10.3",
    "com.azavea.geotrellis" %% "geotrellis-vector" % "0.10.3",
    "com.azavea.geotrellis" %% "geotrellis-util"   % "0.10.3",
    "org.apache.spark"      %% "spark-core"        % "2.0.1" % "provided",
    "org.scalatest"         %% "scalatest"         % "3.0.0" % "test"
)
```

With these in place, even with no Scala source files, a `sbt compile` will
succeed. When you do add source files, put them under `src/main/scala/` as
that's where `sbt` expects them.

It might take a long to complete the first time you do `sbt compile`,
because of all the dependencies it has to fetch.

À la Carte GeoTrellis Modules
-----------------------------

GeoTrellis is actually a library suite made up of many modules. We've
designed it such that you can depend on as much or as little of GeoTrellis
as your project needs. To depend on a new module, add it to the `libraryDependencies`
list in your `build.sbt`:

```scala
libraryDependencies ++= Seq(
    "com.azavea.geotrellis" %% "geotrellis-spark"  % "0.10.3",
    "com.azavea.geotrellis" %% "geotrellis-raster" % "0.10.3",
    "com.azavea.geotrellis" %% "geotrellis-vector" % "0.10.3",
    "com.azavea.geotrellis" %% "geotrellis-util"   % "0.10.3",
    "com.azavea.geotrellis" %% "geotrellis-s3"     % "0.10.3", // now we can use Amazon S3!
    "org.apache.spark"      %% "spark-core"        % "2.0.1" % "provided",
    "org.scalatest"         %% "scalatest"         % "3.0.0" % "test"
)
```

[Click here for a full list and explanation of each GeoTrellis module](../guide/module-hierarchy.md).

Now that you've gotten a simple GeoTrellis environment set up, it's time to
get your feet wet with some of its capabilities.
