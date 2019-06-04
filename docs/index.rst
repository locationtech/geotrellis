What is GeoTrellis?
-------------------

*GeoTrellis* is a Scala library and framework that uses `Apache Spark
<http://spark.apache.org/>`__ to work with raster data. It is released under
the Apache 2 License.

GeoTrellis reads, writes, and operates on raster data as fast as possible.
It implements many `Map Algebra
<http://en.wikipedia.org/wiki/Map_algebra>`__ operations as well as vector
to raster or raster to vector operations.

GeoTrellis also provides tools to render rasters into PNGs or to store
metadata about raster files as JSON. It aims to provide raster processing at
web speeds (sub-second or less) with RESTful endpoints as well as provide
fast batch processing of large raster data sets.

Please visit the `project site <http://geotrellis.io>`__ for more
information as well as some interactive demos.

Why GeoTrellis?
---------------

Raster processing has traditionally been a slow task, which has prompted
advances in vector data processing as an alternative. Raster data isn't
going anywhere, however, with more and more satellite data made public
every year. GeoTrellis is an answer to the growing need for **raster
processing at scale.** We personally have handled terabyte-level data
sets, but really we are only bound by the theoretical limits of Apache
Spark. By *scale* then, we mean *arbitrarily large*.

Contact and Support
-------------------

You can find more information and talk to developers (let us know what
you're working on!) at:

-  `Gitter <https://gitter.im/geotrellis/geotrellis>`__
-  `GeoTrellis mailing list <https://groups.google.com/group/geotrellis-user>`__

Hello Raster!
-------------

Here's a small example showing a routine focal operation over a single
``Tile``:

::

    scala> import geotrellis.raster._
    import geotrellis.raster._

    scala> import geotrellis.raster.render.ascii._
    import geotrellis.raster.render.ascii._

    scala> import geotrellis.raster.mapalgebra.focal._
    import geotrellis.raster.mapalgebra.focal._

    scala> val nd = NODATA
    nd: Int = -2147483648

    scala> val input = Array[Int](
         nd, 7, 1, 1,  3, 5, 9, 8, 2,
          9, 1, 1, 2,  2, 2, 4, 3, 5,
          3, 8, 1, 3,  3, 3, 1, 2, 2,
          2, 4, 7, 1, nd, 1, 8, 4, 3)
    input: Array[Int] = Array(-2147483648, 7, 1, 1, 3, 5, 9, 8, 2, 9, 1, 1, 2,
    2, 2, 4, 3, 5, 3, 8, 1, 3, 3, 3, 1, 2, 2, 2, 4, 7, 1, -2147483648, 1, 8, 4, 3)

    scala> val iat = IntArrayTile(input, 9, 4)  // 9 and 4 here specify columns and rows
    iat: geotrellis.raster.IntArrayTile = IntArrayTile([I@278434d0,9,4)

    // The renderAscii method is mostly useful when you're working with small tiles
    // which can be taken in at a glance.
    scala> iat.renderAscii(AsciiArtEncoder.Palette.STIPLED)
    res0: String =
    ∘█  ▚▜██▖
    █  ▖▖▖▜▚▜
    ▚█ ▚▚▚ ▖▖
    ▖▜█ ∘ █▜▚

    scala> val focalNeighborhood = Square(1)  // a 3x3 square neighborhood
    focalNeighborhood: geotrellis.raster.op.focal.Square =
     O  O  O
     O  O  O
     O  O  O

    scala> val meanTile = iat.focalMean(focalNeighborhood)
    meanTile: geotrellis.raster.Tile = DoubleArrayTile([D@7e31c125,9,4)

    scala> meanTile.getDouble(0, 0)  // Should equal (1 + 7 + 9) / 3
    res1: Double = 5.666666666666667

Ready? `Setup a GeoTrellis development environment. <tutorials/setup.html>`__

.. toctree::
   :maxdepth: 2
   :caption: Home
   :hidden:

   Changelog <CHANGELOG>
   Contributing <CONTRIBUTING>
   Create a Contribution Questionnaire <CQ>

.. toctree::
   :maxdepth: 3
   :caption: Tutorials
   :glob:
   :hidden:

   tutorials/setup
   tutorials/quickstart
   tutorials/kernel-density
   tutorials/reading-geoTiffs

.. toctree::
   :maxdepth: 4
   :caption: User Guide
   :glob:
   :hidden:

   guide/core-concepts
   Using Rasters <guide/rasters>
   Using Vectors <guide/vectors>
   Spark and GeoTrellis <guide/spark>
   Ingesting a Raster Layer <guide/ingests>
   Extending GeoTrellis Types <guide/extending-geotrellis>
   GeoTrellis Module Hierarchy <guide/module-hierarchy>
   Tile Layer Backends <guide/tile-backends>
   Vector Data Backends <guide/vector-backends>
   Frequently Asked Questions <guide/faq>
   Example Archive <guide/examples>

.. toctree::
   :maxdepth: 2
   :caption: Architecture
   :glob:
   :hidden:

   Architecture Decision Records <architecture/adrs>
   Proj4 Implementation <architecture/proj4-implementation>
   architecture/high-performance-scala
   cassandra/cassandra-test.rst
