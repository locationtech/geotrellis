Geotrellis Quickstart Guide
===========================

Geotrellis is a large and evolving package, which makes it difficult to maintain current documentation.  To make up for this deficiency, this quickstart guide will provide a set of documents describing case studies that accomplish some simple goals.  These examples are meant to highlight the fundamental Geotrellis classes as well as show some basic Scala techniques for those new to the language.

Basic Geotrellis Types
----------------------

Geotrellis itself is a library for working with raster data over maps.  Raster data is essentially a regular, rectangular grid containing scalar values (i.e., numerical data) in each grid cell.

The base type for these grid structures is `CellGrid` which offers basic dimension information.  The `Tile` trait gives the general interface for working with data over a grid.  Actual usage of Tiles requires an understanding of the NoData handling options defined in `CellType` and described in more detail [elsewhere](../raster/celltype.md) in the documentation.  Generally speaking, though, for Tiles containing integer data types (including bits and bytes), the `IntArrayTile` companion object is used to construct Tiles, while floating point Tiles are created using the `DoubleArrayTile` companion object.

A Tile needs additional context if it is to represent data on a map; specifically, it needs an `Extent` which defines the map coordinates of the corners of the Tile.  `Extent(xmin,ymin,xmax,ymax)` represents the bounding box of the Tile *expressed in some coordinate system*.  To apply the Tile to a map, it'll be necessary to define which coordinate system was used, though that will happen at a later stage.  A Tile and Extent come together to form a `Raster.`

In addition to Raster data, Geotrellis also allows the construction of geometric, or vector, objects for use on maps.  These include the `Point`, `Line`, `Polygon` and any other subclass of `Geometry.`  Often, vector objects will have some data associated with them, in which case, it will be necessary to combine the two using the `Feature` class.  Several companion objects exist for streamlining the creation of Features, such as `PointFeature.`  Additional detail may be found [here](../vector/vector-intro.md).

Kernel Density
--------------

This first example will demonstrate how to build a raster from point data using kernel density estimation.  In this process, at every point in a sample, the contents of what is effectively a small Tile (called a Kernel) containing a predefined pattern are added to the grid cells surrounding the point in question (i.e., the kernel is centered on the tile cell containing the point and then added to the Tile).  If the kernel is Gaussian, this can develop a smooth approximation to the density function that the points were sampled from, assuming that the points were sampled according to a density function.  (Alternatively, each point can be given a weight, and the kernel values can be scaled by that weight before being applied to the tile, which we will do below.)

To begin, let's generate a random collection of points with weights in the range (0,100).

```scala
    import scala.util._

    val extent = Extent.fromString("-109, 37, -102, 41") // Extent of Colorado

    def randomPointFeature(extent: Extent): PointFeature[Double] = {
      def randInRange (low: Double, high: Double): Double = {
        val x = Random.nextDouble
        low * (1-x) + high * x
      }
      PointFeature(Point(randInRange(extent.xmin, extent.xmax),
                         randInRange(extent.ymin, extent.ymax)), 
                   Random.nextInt % 50 + 50)
    }

    val pts = (for (i <- 1 to 1000) yield randomPointFeature(extent)).toList
```

The choice of extent is largely arbitrary in this example, but note that the coordinates here are taken with respect to the standard (longitude, latitude) that we normally consider.  Other coordinate representations are available, and it might be useful to investigate coordinate reference systems if you want more details.  Some operations in Geotrellis require that a CRS object be constructed to place your rasters in the proper context.  For (longitude, latitude) coordinates, either `CRS.fromName("EPSG:4326")` or `LatLng` will generate the desired CRS.

Next, we will create a tile containing the kernel density estimate:
```scala
    import geotrellis.raster.mapalgebra.focal.Kernel

    val kernelWidth = 9
    val kern = Kernel.gaussian(kernelWidth, 1.5, 64) // Gaussian kernel of width 9, std. deviation 1.5, amplitude 64
    val kde = pts.kernelDensity (kern, RasterExtent(extent, 700, 400))
```

This populates a 700x400 tile with the desired kernel density estimate.  In order to view the resulting file, a simple method is to write the tile out to PNG or TIFF.  The advantage of the latter is that it will be tagged with the extents and CRS, and the resulting image file can be overlayed on a map in a viewing program such as QGIS.
```scala
    import geotrellis.raster.render._
    import geotrellis.raster.io.geotiff._

    // PNG output
    kde.renderPng().write("test.png")

    // TIF output
    GeoTiff(kde, extent, CRS.fromName("EPSG:4326")).write("test.tif")
```

### Subdividing Tiles ###

The above example focuses on a toy problem that creates a small raster object. However, the purpose of Geotrellis is to enable the processing of large-scale datasets.  In order to work with large rasters, it will be necessary to subdivide a region into a grid of tiles so that the processing of each piece may be handled by different processors which may, for example, reside on remote machines in a cluster.  This section explains some of the concepts involved in subdividing a raster into a set of tiles.

We will still use an `Extent` object to set the bounds of our raster patch in space, but we must now specify how that extent is broken up into a grid of `Tile`s.  This requires a statement of the form
```scala
    val tl = TileLayout(7, 4, 100, 100)
```
Here, we have specified a 7x4 grid of Tiles, each of which has 100x100 cells. This will eventually be used to divide the earlier monolithic 700x400 Tile (`kde`) into 28 uniformly-sized subtiles.  The TileLayout is then combined with the extent in a `LayoutDefinition` object:
```scala
    val ld = LayoutDefinition(extent, tl)
```

In preparation for reimplementing the previous kernel density estimation with this structure, we note that each point in `pts` lies at the center of a kernel, which covers some non-zero area around the points.  We can think of each point/kernel pair as a small square extent centered at the point in question with a side length of 9 pixels (the arbitrary width we chose for our kernel earlier).  Each pixel, however, covers some non-zero area of the map, which we can also think of as an extent with side lengths given in map coordinates.  The dimensions of a pixel's extent are given by the `cellwidth` and `cellheight` members of a LayoutDefinition object.

By incorporating all these ideas, we can create the following function to generate the extent of the kernel centered at a given point:
```scala
    import geotrellis.spark.tiling._

    def pointFeatureToExtent[D](krnwdth: Double, ld: LayoutDefinition, ptf: PointFeature[D]) : Extent = {
      val p = ptf.geom
      Extent(p.x - kernelWidth * ld.cellwidth / 2,
             p.y - kernelWidth * ld.cellheight / 2,
             p.x + kernelWidth * ld.cellwidth / 2,
             p.y + kernelWidth * ld.cellheight / 2)
    }
    val ptfToExtent = { p: PointFeature[Double] => pointFeatureToExtent(9, ld, p) }
```

When we consider the kernel extent of a point in the context of a LayoutDefinition, it's clear that a kernel's extent may overlap more than one tile in the layout.  To discover the tiles that a given point's kernel extents overlap, LayoutDefinition provides the `mapTransform` member.  Among the methods of mapTransform is the ability to determine the indices of tiles in the TileLayout overlap a given extent.  Note that the tiles in a given layout are indexed by `SpatialKey`s, which are effectively `(Int,Int)` pairs giving the (column,row) of each tile as follows:
```
    +-------+-------+-------+       +-------+
    | (0,0) | (1,0) | (2,0) | . . . | (6,0) |
    +-------+-------+-------+       +-------+
    | (0,1) | (1,1) | (2,1) | . . . | (6,1) |
    +-------+-------+-------+       +-------+
        .       .       .     .         .
        .       .       .       .       .
        .       .       .         .     .
    +-------+-------+-------+       +-------+
    | (0,3) | (1,3) | (2,3) | . . . | (6,3) |
    +-------+-------+-------+       +-------+
```
Specifically, in our running example, `ld.mapTransform(ptfToExtent(Feature(Point(-108, 38), 100.0)))` returns `GridBounds(0, 2, 1, 3),` indicating that every cell with columns in the range [0,1] and rows in the range [2,3] are intersected by the kernel centered on the point (-108, 38)---that is, the 2x2 block of tiles at the lower left of the layout.

In order to proceed with the kernel density estimation, it is necessary to then convert the list of points into a collection of `(SpatialKey,List[PointFeature[Double]])` that gathers all the points that have an effect on each subtile, as indexed by their SpatialKeys.  The following snippet accomplishes that.
```scala
      def ptfToSpatialKey[D](ptf: PointFeature[D]) :
          Seq[(SpatialKey,PointFeature[D])] = {
        val ptextent = ptfToExtent(ptf)
        val gridBounds = ld.mapTransform(ptextent)
        for ((c,r) <- gridBounds.coords;
             if r < tl.totalRows;
             if c < tl.totalCols) yield (SpatialKey(c,r), ptf)
      }

      val keyfeatures = pts.flatMap(ptfToSpatialKey).groupBy(_._1).map {
        case (sk,v) => (sk,v.unzip._2)
      }: Map[SpatialKey,List[PointFeature[Double]]]
```

Now, all the subtiles may be generated in the same fashion as the monolithic tile case above.

```scala
      val keytiles = keyfeatures.map { 
        case (sk,pfs) => (sk,kernelDensity (pfs,trans,kern,
                                            RasterExtent(ld.mapTransform(sk),
                                                         tl.tileDimensions._1,
                                                         tl.tileDimensions._2))) 
      }
```

Finally, it is necessary to combine the results.  Note that, in order to produce a 700x400 tile that is identical to the simpler, non-tiled case, every SpatialKey must be represented in the map, or the result may not span the full range.  This is only necessary if it is important to generate a tile that spans the full extent.

```scala
      val tileList = 
        for { r <- 0 until ld.layoutRows
            ; c <- 0 until ld.layoutCols
            } yield {
              val k = SpatialKey(c,r)
              (k, keytiles.getOrElse(k, IntArrayTile.empty(tl.tileCols, 
                                                           tl.tileRows)))
            }
      val stitched = TileLayoutStitcher.stitch(tileList)._1
```

It is now the case that `stitched` is identical to `kde.`

#### A note on implementation ####

The procedures that we've been considering above have been implemented in GeoTrellis and are located in `raster/





