Geotrellis Quickstart Guide
===========================

Geotrellis is a vast and evolving package, which makes it difficult to
maintain current documentation.  To make up for this deficiency, this
quickstart guide will provide a set of documents describing case studies that
accomplish some simple goals.  These examples are meant to highlight the
fundamental Geotrellis classes as well as show some basic Scala techniques for
those new to the language.

Basic Geotrellis Types
----------------------

Geotrellis itself is a library for working with raster data over maps.  Raster
data is essentially a regular, rectangular grid containing scalar values
(i.e., numerical data) in each grid cell.

The base type for these grid structures is `CellGrid` which offers basic
dimension information.  The `Tile` trait gives the general interface for
working with data over a grid.  Actual usage of Tiles requires an
understanding of the NoData handling options defined in `CellType` and
described in more detail [elsewhere](../raster/celltype.md) in the
documentation.  Generally speaking, though, for Tiles containing integer data
types (including bits and bytes), the `IntArrayTile` companion object is used
to construct Tiles, while floating point Tiles are created using the
`DoubleArrayTile` companion object.

A Tile needs additional context if it is to represent data on a map;
specifically, it needs an `Extent` which defines the map coordinates of the
corners of the Tile.  `Extent(xmin,ymin,xmax,ymax)` represents the bounding
box of the Tile *expressed in some coordinate system*.  To apply the Tile to a
map, it'll be necessary to define which coordinate system was used, though
that will happen at a later stage.  A Tile and Extent come together to form a
`Raster.`

In addition to Raster data, Geotrellis also allows the construction of
geometric, or vector, objects for use on maps.  These include the `Point`,
`Line`, `Polygon` and any other subclass of `Geometry.`  Often, vector
objects will have some data associated with them, in which case, it will be
necessary to combine the two using the `Feature` class.  Several companion
objects exist for streamlining the creation of Features, such as
`PointFeature.`  Additional detail may be found
[here](../vector/vector-intro.md).

Kernel Density
--------------

This first example will demonstrate how to build a raster from point data
using kernel density estimation.  In this process, at every point in a sample,
the contents of a small Tile containing a predefined pattern (this is the
kernel) are added to the grid cells surrounding the point in question (i.e.,
the kernel is centered on the tile cell containing the point and then added to
the Tile).  If the kernel is Gaussian, this can develop a smooth approximation
to the density function that the points were sampled from, assuming that the
points were sampled according to a density function.  (Alternatively, each
point can be given a weight, and the kernel values can be scaled by that
weight before being applied to the tile, which we will do below.)

To begin, let's generate a random collection of points with weights in the
range (0,100).

```scala
    import scala.util._

    val extent = Extent.fromString("-109,37,-102,41") // Extent of Colorado

    def randomPointFeature(extent: Extent) : PointFeature[Double] = {
      def randInRange (low : Double, high : Double) : Double = {
        val x = Random.nextDouble
        low * (1-x) + high * x
      }
      PointFeature(Point(randInRange(extent.xmin,extent.xmax),
                         randInRange(extent.ymin,extent.ymax)), 
                   Random.nextInt % 50 + 50)
    }

    val pts = (for (i <- 1 to 1000) yield randomPointFeature(extent)).toList
```

The choice of extent is largely arbitrary in this example, but note that the
coordinates here are taken with respect to the standard (longitude, latitude)
that we normally consider.  Other coordinate representations are available,
and it might be useful to investigate coordinate reference systems if you want
more details.  Some operations in Geotrellis require that a CRS object be
constructed to place your rasters in the proper context.  For long/lat
coordinates, `CRS.fromName("EPSG:4326")` will generate the desired CRS.

Next, we will create a tile containing the kernel density estimate:
```scala
    import geotrellis.raster.mapalgebra.focal.Kernel

    val kern = Kernel.gaussian(9,1.5,64)
    val trans = (_.toFloat.round.toInt) : Double => Int
    val kde = VectorToRaster.kernelDensity (pts,trans,kern,RasterExtent(extent,700,400))
```

This populates a 700x400 tile with the desired kernel density estimate.  In
order to view the resulting file, a simple method is to write the tile out to
PNG or TIFF.  The advantage of the latter is that it will be tagged with the
extents and CRS, and the resulting image file can be overlayed on a map in a
viewing program such as QGIS.
```scala
    import geotrellis.raster.render._
    import geotrellis.raster.io.geotiff._

    // PNG output
    kde.renderPng().write("test.png")

    // TIF output
    GeoTiff(kde,extent,CRS.fromName("EPSG:4326")).write("test.tif")
```

