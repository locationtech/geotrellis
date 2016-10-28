GeoTrellis Quickstart Guide
===========================

GeoTrellis is a free and open source library that can be obtained easily on [GitHub](https://github.com/geotrellis/geotrellis) using `git` (look [here](https://git-scm.com) for more detail).  But for most users, it is not necessary to download the GeoTrellis source code to make use of it.  The purpose of this document is to describe the fastest means to get a running environment for various use cases.

Wetting Your Feet
-----------------

By far, the quickest route to being able to play with GeoTrellis is to follow these steps:

1. Use `git` to clone the template repository at `github.com/geotrellis/geotrellis-sbt-template repository`:
```bash
  git clone git@github.com:geotrellis/geotrellis-sbt-template
```
2. Once available, from the root directory of the cloned repo, MacOSX and Linux users may launch the `sbt` script contained therein; this will start an SBT session, installing it if it has not already been.
3. Once SBT is started, issue the `console` command; this will start the Scala interpreter.

At this point, you should be able to issue the command `import geotrellis.vector._` without raising an error.  This will make the contents of that package available.  For instance, one may create a point at the origin by typing `Point(0, 0)`.

This same project can be used as a template for writing simple programs.  Under the project root directory is the `src` directory which has subtrees rooted at `src/main` and `src/test`.  The former is where application code should be located, and the latter contains unit tests for any modules that demand it.  The SBT documentation will describe how to run application or test code.

Using GeoTrellis with Apache Spark
---------------------------------

GeoTrellis is meant for use in distributed environments employing Apache Spark.  It's beyond the scope of a quickstart guide to describe how to set up or even to use Spark, but there are two paths to getting a REPL in which one can interact with Spark.

First: from the `geotrellis/geotrellis-sbt-template` project root directory, issue `./sbt` to start SBT.  Once SBT is loaded, issue the `test:console` command.  This will raise a REPL that will allow for the construction of a SparkContext using the following commands:
```scala
  val conf = new org.apache.spark.SparkConf()
  conf.setMaster("local[*]")
  implicit val sc = geotrellis.spark.util.SparkUtils.createSparkContext("Test console", conf)
```
It will then be possible to issue a command such as `sc.parallelize(Array(1,2,3))`.

Alternatively, if you have source files inside a project directory tree (perhaps derived from `geotrellis-sbt-template`), you may issue the `assembly` command from `sbt` to produce a fat .jar file, which will appear in the `target/scala-<version>/` directory.  That jar file can be supplied to `spark-shell --jars <jarfile>`, given you have Spark installed on your local machine.  That same jar file could be supplied to `spark-submit` if you are running on a remote Spark master.  Again, the ins-and-outs of Spark are beyond the scope of this document, but these pointers might provide useful jumping off points.

