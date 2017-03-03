Setup
=====

Welcome to GeoTrellis, the `Scala <http://www.scala-lang.org/>`__
library for high-performance geographic data processing. Being a
library, users import GeoTrellis and write their own Scala applications
with it. This guide will help you get up and running with a basic
GeoTrellis development environment.

Requirements
------------

-  `Java
   8 <http://www.oracle.com/technetwork/java/javase/overview/index.html>`__.
   GeoTrellis code won't function with Java 7 or below. You can test
   your Java version by entering the following in a Linux or Mac
   terminal:

.. code:: console

    > javac -version
    javac 1.8.0_102

You want to see ``1.8`` like above.

-  `Apache Spark 2 <http://spark.apache.org/downloads.html>`__. This is
   if you plan to run ingests (as shown in our `ETL
   tutorial <./etl-tutorial.html>`__) or write a serious application.
   Otherwise, fetching Spark dependencies for playing with GeoTrellis is
   handled automatically, as shown in our `Quick-Start Guide <./quickstart.html>`__.

When running more involved applications, ``spark-submit`` should be on
your ``PATH``:

.. code:: console

    > which spark-submit
    /bin/spark-submit

Using Scala
-----------

GeoTrellis is a Scala library, so naturally you must write your
applications in Scala. If you're new to Scala, we recommend the
following:

-  The `official Scala
   tutorials <http://www.scala-lang.org/documentation/>`__
-  The `Scala
   Cookbook <http://shop.oreilly.com/product/0636920026914.do>`__ as a
   handy language reference
-  `99 Problems in Scala <http://aperiodic.net/phil/scala/s-99/>`__ to
   develop basic skills in Functional Programming

GeoTrellis Project Template
---------------------------

The
`geotrellis-sbt-template <https://github.com/geotrellis/geotrellis-sbt-template>`__
repo provides a simple GeoTrellis project template. It can be used to
experiment with GeoTrellis, or to write full applications. Get it with:

.. code:: console

    git clone https://github.com/geotrellis/geotrellis-sbt-template.git

You don't need ``sbt`` installed to write a GeoTrellis app, since this
template includes an ``sbt`` bootstrap script. It is used like regular
SBT, and comes with a few extra commands:

-  Enter the SBT shell: ``./sbt``
-  Run tests: ``./sbt test``
-  Force Scala 2.11 (default): ``./sbt -211``
-  Force Scala 2.10: ``./sbt -210``

À la Carte GeoTrellis Modules
-----------------------------

GeoTrellis is actually a library suite made up of many modules. We've
designed it such that you can depend on as much or as little of
GeoTrellis as your project needs. To depend on a new module, add it to
the ``libraryDependencies`` list in your ``build.sbt``:

.. code:: scala

    libraryDependencies ++= Seq(
        "org.locationtech.geotrellis" %% "geotrellis-spark"  % "1.0.0",
        "org.locationtech.geotrellis" %% "geotrellis-s3"     % "1.0.0", // now we can use Amazon S3!
        "org.apache.spark"            %% "spark-core"        % "2.1.0" % "provided",
        "org.scalatest"               %% "scalatest"         % "3.0.0" % "test"
    )

`Click here for a full list and explanation of each GeoTrellis
module <../guide/module-hierarchy.html>`__.

Now that you've gotten a simple GeoTrellis environment set up, it's time
to get your feet wet with some of its capabilities.
