Quick Start
===========

For most users, it is not necessary to download the GeoTrellis source
code to make use of it. The purpose of this document is to describe the
fastest means to get a running environment for various use cases.

Wetting your Feet
-----------------

By far, the quickest route to being able to play with GeoTrellis is to
follow these steps:

-  Use ``git`` to clone our `project template
   repository <https://github.com/geotrellis/geotrellis-sbt-template>`__:

.. code:: console

    git clone git@github.com:geotrellis/geotrellis-sbt-template

-  Once available, from the root directory of the cloned repo, MacOSX
   and Linux users may launch the ``sbt`` script contained therein; this
   will start an SBT session, installing it if it has not already been.
-  Once SBT is started, issue the ``console`` command; this will start
   the Scala interpreter.

At this point, you should be able to issue the command
``import geotrellis.vector._`` without raising an error. This will make
the contents of that package available. For instance, one may create a
point at the origin by typing ``Point(0, 0)``.

This same project can be used as a template for writing simple programs.
Under the project root directory is the ``src`` directory which has
subtrees rooted at ``src/main`` and ``src/test``. The former is where
application code should be located, and the latter contains unit tests
for any modules that demand it. The SBT documentation will describe how
to run application or test code.

Hello Raster, Revisited
-----------------------

On the `landing page <../index.html>`__, an example of an interactive
session with GeoTrellis was shown. We're going to revisit that example
here in more detail, using the various parts of that example as a means
to highlight library features and to marshal beginners to the sections
of interest in the documentation.

It is first necessary to expose functionality from the relevant packages
(a complete list packages and the summary of their contents may be found
`here <../guide/module-hierarchy.html>`__):

.. code:: scala

    scala> import geotrellis.raster._
    import geotrellis.raster._

    scala> import geotrellis.raster.mapalgebra.focal._
    import geotrellis.raster.mapalgebra.focal._

Much of GeoTrellis' core functionality lies in the raster library.
`Rasters <../guide/core-concepts.html#raster-data>`__ are regular grids of
data that have some notion of their spatial extent. When working with
rasters, one can operate on the grid of data separately from the spatial
information. The grid of data held inside a raster is called a Tile. We
can create an example Tile as follows:

.. code:: scala

    scala> val nd = NODATA
    nd: Int = -2147483648

    scala> val input = Array[Int](
         nd, 7, 1, 1, 3, 5, 9, 8, 2,
         9, 1, 1, 2, 2, 2, 4, 3, 5,
         3, 8, 1, 3, 3, 3, 1, 2, 2,
         2, 4, 7, 1, nd, 1, 8, 4, 3)
    input: Array[Int] = Array(-2147483648, 7, 1, 1, 3, 5, 9, 8, 2, 9, 1, 1, 2,
    2, 2, 4, 3, 5, 3, 8, 1, 3, 3, 3, 1, 2, 2, 2, 4, 7, 1, -2147483648, 1, 8, 4, 3)

    scala> val iat = IntArrayTile(input, 9, 4)  // 9 and 4 here specify columns and rows
    iat: geotrellis.raster.IntArrayTile = IntArrayTile(I@278434d0,9,4)

    // The asciiDraw method is mostly useful when you're working with small tiles
    // which can be taken in at a glance
    scala> iat.asciiDraw()
    res0: String =
    "    ND     7     1     1     3     5     9     8     2
         9     1     1     2     2     2     4     3     5
         3     8     1     3     3     3     1     2     2
         2     4     7     1    ND     1     8     4     3
    "

Note that not every cell location in a tile needs to be specified; this
is the function of ``NODATA``. Also be aware that ``NODATA``'s value
varies by `CellType <../guide/core-concepts.html#cell-types>`__. In this
case, the use of ``IntArrayTile`` implies an ``IntCellType`` which
defines ``NODATA`` as seen above.

As a GIS package, GeoTrellis provides a number of `map
algebra <../guide/core-concepts.html#map-algebra>`__ operations. In the
following example, a neighborhood is defined as the region of interest
for a focal operation, the focal mean operation is performed, and a
value is queried:

.. code:: scala

    scala> val focalNeighborhood = Square(1)  // a 3x3 square neighborhood
    focalNeighborhood: geotrellis.raster.op.focal.Square =
     O  O  O
     O  O  O
     O  O  O

    scala> val meanTile = iat.focalMean(focalNeighborhood)
    meanTile: geotrellis.raster.Tile = DoubleArrayTile(D@7e31c125,9,4)

    scala> meanTile.getDouble(0, 0)  // Should equal (1 + 7 + 9) / 3
    res1: Double = 5.666666666666667

In this example, note that the NODATA value was simply ignored in the
computation of the mean.

This is only a very simple example of what is possible with GeoTrellis.
To learn more, it is recommended that the reader continue on with the
`core concepts <../guide/core-concepts.html>`__ section. Another example
geared towards new users is available in the `kernel density
tutorial <../tutorials/kernel-density.html>`__.

Using GeoTrellis with Apache Spark
----------------------------------

GeoTrellis is meant for use in distributed environments employing Apache
Spark. It's beyond the scope of a quickstart guide to describe how to
set up or even to use Spark, but there are two paths to getting a REPL
in which one can interact with Spark.

First: from the ``geotrellis/geotrellis-sbt-template`` project root
directory, issue ``./sbt`` to start SBT. Once SBT is loaded, issue the
``test:console`` command. This will raise a REPL that will allow for the
construction of a SparkContext using the following commands:

.. code:: scala

      val conf = new org.apache.spark.SparkConf()
      conf.setMaster("local[*]")
      implicit val sc = geotrellis.spark.util.SparkUtils.createSparkContext("Test console", conf)

It will then be possible to issue a command such as
``sc.parallelize(Array(1,2,3))``.

Alternatively, if you have source files inside a project directory tree, you
may issue the ``assembly`` command from ``sbt`` to produce a fat .jar file,
which will appear in the ``target/scala-<version>/`` directory under your
current project's root directory. That jar file can be supplied to the Spark
shell as part of the command ``spark-shell --conf
spark.serializer=org.apache.spark.serializer.KryoSerializer --jars
<jarfile>``, provided you have Spark installed on your local machine.  Fat
jars created via ``assembly`` can be supplied as well to ``spark-submit``
commands for running jobs on a remote Spark master.  Again, the ins-and-outs
of Spark are beyond the scope of this document, but these pointers might
provide useful jumping off points.
