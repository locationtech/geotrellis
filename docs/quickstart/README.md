GeoTrellis Quickstart Guide
===========================

GeoTrellis is a large and evolving package, which makes it difficult to maintain current documentation.
To make up for this deficiency, this quickstart guide will provide a set of documents describing case studies that accomplish some simple goals.
These examples are meant to highlight the fundamental GeoTrellis classes as well as show some basic Scala techniques for those new to the language.

Getting Started
---------------

In the following, we'll be providing some Scala code samples that make use of the GeoTrellis library.
This library can be obtained easily on [GitHub](https://github.com/geotrellis/geotrellis) using `git`, which is available for free (with copious documentation) [here](https://git-scm.com).
The process for installing GeoTrellis is as follows:
1. Begin by using `git` to clone the repository at `github.com/geotrellis/geotrellis-sbt-template repository`.
2. Once available, from the root directory of the cloned repo, MacOSX and Linux users may launch the sbt script contained therein; this will start an SBT session, installing it if it has not already been.
3. Once SBT is started, issue the `console` command; this will start the Scala interpreter.
At this point, you should be able to issue the command `import geotrellis.vector._` without raising an error.

Basic GeoTrellis Types
----------------------

GeoTrellis itself is a library for working with raster data over maps.
Raster data is essentially a regular, rectangular grid containing scalar values (i.e., numerical data) in each grid cell.

The base type for these grid structures is `CellGrid` which offers basic dimension information.
The `Tile` trait gives the general interface for working with data over a grid.
Actual usage of Tiles requires an understanding of the NoData handling options defined in `CellType` and described in more detail [elsewhere](../raster/celltype.md) in the documentation.
Generally speaking, though, for Tiles containing integer data types (including bits and bytes), the `IntArrayTile` companion object is used to construct integer-valued Tiles, while Tiles of floating point values are created using the `DoubleArrayTile` companion object.

A Tile needs additional context if it is to represent data on a map; specifically, it needs an `Extent` which defines the map coordinates of the corners of the Tile.
`Extent(xmin,ymin,xmax,ymax)` represents the bounding box of the Tile *expressed in some coordinate system*.
In order to properly place the contents of a Tile on some map, for some subsequent computation or for display purposes, it'll be necessary to define which coordinate system was used, though that will happen at a later stage.
A Tile and Extent come together to form a `Raster.`

In addition to Raster data, GeoTrellis also allows the construction of geometric, or vector, objects for use on maps.
These include the `Point`, `Line`, `Polygon` and any other subclass of `Geometry.`
Often, vector objects will have some data associated with them, in which case, it will be necessary to combine the two using the `Feature` class.
Several companion objects exist for streamlining the creation of Features, such as `PointFeature.`
Additional detail may be found [here](../vector/vector-intro.md).

Kernel Density
--------------

This first example will demonstrate how to build a raster from point data using kernel density estimation.
In this process, at every point in a sample, the contents of what is effectively a small Tile (called a Kernel) containing a predefined pattern are added to the grid cells surrounding the point in question (i.e., the kernel is centered on the tile cell containing the point and then added to the Tile).
If the kernel is Gaussian, this can develop a smooth approximation to the density function that the points were sampled from, assuming that the points were sampled according to a density function.
(Alternatively, each point can be given a weight, and the kernel values can be scaled by that weight before being applied to the tile, which we will do below.)

To begin, let's generate a random collection of points with weights in the range (0, 32).

```scala
  import geotrellis.vector._
  import scala.util._

  val extent = Extent.fromString("-109, 37, -102, 41") // Extent of Colorado

  def randomPointFeature(extent: Extent): PointFeature[Double] = {
    def randInRange (low: Double, high: Double): Double = {
      val x = Random.nextDouble
      low * (1-x) + high * x
    }
    Feature(Point(randInRange(extent.xmin, extent.xmax),
                  randInRange(extent.ymin, extent.ymax)), 
            Random.nextInt % 16 + 16)
  }

  val pts = (for (i <- 1 to 1000) yield randomPointFeature(extent)).toList
```

The choice of extent is largely arbitrary in this example, but note that the coordinates here are taken with respect to the standard (longitude, latitude) that we normally consider.
Other coordinate representations are available, and it might be useful to investigate coordinate reference systems (CRSs) if you want more details.
Some operations in GeoTrellis require that a CRS object be constructed to place your rasters in the proper context.
For (longitude, latitude) coordinates, either `CRS.fromName("EPSG:4326")` or `LatLng` will generate the desired CRS.

Next, we will create a tile containing the kernel density estimate:
```scala
  import geotrellis.raster.mapalgebra.focal.Kernel

  val kernelWidth = 9
  val kern = Kernel.gaussian(kernelWidth, 1.5, 25) // Gaussian kernel with std. deviation 1.5, amplitude 25
  val kde = pts.kernelDensity (kern, RasterExtent(extent, 700, 400))
```

This populates a 700x400 tile with the desired kernel density estimate.
In order to view the resulting file, a simple method is to write the tile out to PNG or TIFF.
In the following snippet, a PNG is created where the values of the tile are mapped to colors that smoothly interpolate from blue to yellow to red.
```scala
  import geotrellis.raster.render._

  val colorMap = ColorMap((0 to kde.findMinMax._2 by 4).toArray, ColorRamps.HeatmapBlueToYellowToRedSpectrum)
  kde.renderPng(colorMap).write("test.png")
```

The advantage of using a TIFF output is that it will be tagged with the extent and CRS, and the resulting image file can be overlayed on a map in a viewing program such as QGIS.
That output is generated by the following statements.
```scala
  import geotrellis.raster.io.geotiff._

  GeoTiff(kde, extent, LatLng).write("test.tif")
```

### Subdividing Tiles ###

The above example focuses on a toy problem that creates a small raster object. However, the purpose of GeoTrellis is to enable the processing of large-scale datasets.
In order to work with large rasters, it will be necessary to subdivide a region into a grid of tiles so that the processing of each piece may be handled by different processors which may, for example, reside on remote machines in a cluster.
This section explains some of the concepts involved in subdividing a raster into a set of tiles.

We will still use an `Extent` object to set the bounds of our raster patch in space, but we must now specify how that extent is broken up into a grid of `Tile`s.
This requires a statement of the form
```scala
  val tl = TileLayout(7, 4, 100, 100)
```
Here, we have specified a 7x4 grid of Tiles, each of which has 100x100 cells. This will eventually be used to divide the earlier monolithic 700x400 Tile (`kde`) into 28 uniformly-sized subtiles.
The TileLayout is then combined with the extent in a `LayoutDefinition` object:
```scala
  val ld = LayoutDefinition(extent, tl)
```

In preparation for reimplementing the previous kernel density estimation with this structure, we note that each point in `pts` lies at the center of a kernel, which covers some non-zero area around the points.
We can think of each point/kernel pair as a small square extent centered at the point in question with a side length of 9 pixels (the arbitrary width we chose for our kernel earlier).
Each pixel, however, covers some non-zero area of the map, which we can also think of as an extent with side lengths given in map coordinates.
The dimensions of a pixel's extent are given by the `cellwidth` and `cellheight` members of a LayoutDefinition object.

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
  def ptfToExtent[D](p: PointFeature[D]) = pointFeatureToExtent(9, ld, p)
```

When we consider the kernel extent of a point in the context of a LayoutDefinition, it's clear that a kernel's extent may overlap more than one tile in the layout.
To discover the tiles that a given point's kernel extents overlap, LayoutDefinition provides a `mapTransform` object.
Among the methods of mapTransform is the ability to determine the indices of the subtiles in the TileLayout that overlap a given extent.
Note that the tiles in a given layout are indexed by `SpatialKey`s, which are effectively `(Int,Int)` pairs giving the (column,row) of each tile as follows:
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

In order to proceed with the kernel density estimation, it is necessary to then convert the list of points into a collection of `(SpatialKey, List[PointFeature[Double]])` that gathers all the points that have an effect on each subtile, as indexed by their SpatialKeys.
The following snippet accomplishes that.
```scala
    def ptfToSpatialKey[D](ptf: PointFeature[D]) :
        Seq[(SpatialKey,PointFeature[D])] = {
     val ptextent = ptfToExtent(ptf)
      val gridBounds = ld.mapTransform(ptextent)
      for ((c, r) <- gridBounds.coords;
           if r < tl.totalRows;
           if c < tl.totalCols) yield (SpatialKey(c,r), ptf)
    }

    val keyfeatures: Map[SpatialKey, List[PointFeature[Double]]] =
      pts
        .flatMap(ptfToSpatialKey)
        .groupBy(_._1)
        .map { case (sk, v) => (sk, v.unzip._2) }
```

Now, all the subtiles may be generated in the same fashion as the monolithic tile case above.

```scala
    val keytiles = keyfeatures.map { 
      case (sk, pfs) => (sk, pfs.kernelDensity (kern,
                                                RasterExtent(ld.mapTransform(sk),
                                                             tl.tileDimensions._1,
                                                             tl.tileDimensions._2))) 
    }
```

Finally, it is necessary to combine the results.
Note that, in order to produce a 700x400 tile that is identical to the simpler, non-tiled case, every SpatialKey must be represented in the map, or the result may not span the full range.
This is only necessary if it is important to generate a tile that spans the full extent.

```scala
  import geotrellis.spark.stitch.TileLayoutStitcher

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

### Distributing Computation via Apache Spark ###

As mentioned, the reason for introducing this more complicated version of kernel density estimation was to enable distributed processing of the kernel stamping process.
Each subtile could potentially be handled by a different node in a cluster, which would make sense if the dataset in question were, say, the location of individual trees, requiring a huge amount of working memory.
By breaking the domain into smaller pieces, we can exploit the distributed framework provided by Apache Spark to spread the task to a number of machines.
This will also provide fault tolerant, rapid data processing for real-time and/or web-based applications.
Spark is much too big a topic to discuss in any detail here, so the curious reader is advised to search the web for more information.

Our concern falls on how to write code to exploit the structures provided by Spark, specifically *Resilient Distributed Datasets*, or RDDs.
An RDD is a distributed collection, with all of the usual features of a collection—e.g., map, reduce—as well as distributed versions of certain sequential operations—e.g., `aggregate` is akin to `foldLeft` for standard collections.
In order to create an RDD, one will call the `parallelize()` method on a `SparkContext` object, which can be generated by the following set of statements.
```scala
  import org.apache.spark.{SparkConf, SparkContext}
  val conf = new SparkConf().setMaster("local").setAppName("Kernel Density")
  val sc = new SparkContext(conf)
```

In our case, we have a collection of PointFeatures that we wish to parallelize, so we issue the command
```scala
  import org.apache.spark.rdd.RDD
  val pointRdd = sc.parallelize(pts, 10)
```
Here, the 10 indicates that we want to distribute the data, as 10 partitions, to the available workers.
A partition is a subset of the data in an RDD that will be processed by one of the workers, enabling parallel, distributed computation, assuming the existence of a pool of workers on a set of machines.
If we exclude this value, the default parallelism will be used, which is typically the number of processors, though in this local example, it defaults to one.

In order to perform the same task as in the previous section, but in parallel, we will approach the problem in much the same way: points will be assigned an extent corresponding to the extent of the associated kernel, those points will be assigned SpatialKeys based on which subtiles their kernels overlap, and each kernel will be applied to the tile corresponding to its assigned SpatialKey.
Earlier, this process was effected by a flatMap followed by a groupBy and then a map.
This very same procedure could be used here, save for the fact that groupBy, when applied to an RDD, triggers an expensive, slow, network-intensive shuffling operation which collects items with the same key on a single node in the cluster.
Instead, a fold-like operation will be used: `aggregateByKey`, which has a signature of <center style="padding-bottom: 1em">`RDD[(K, U)] => T => ((U, T) => T, (T, T) => T) => RDD[(K, T)]`.</center>
That is, we begin with an RDD of key/value pairs, provide a “zero value” of type `T`, the type of the final result, and two functions: (1) a *sequential operator*, which uses a single value of type `U` to update an accumulated value of type `T`, and (2) a *combining operator*, which merges two accumulated states of type `T`.
In our case, `U = PointFeature[Double]` and `T = Tile`; this implies that the insertion function is a kernel stamper and the merging function is a tile adder.

```scala
  import geotrellis.raster.density.KernelStamper

  def stampPointFeature(tile: MutableArrayTile, tup: (SpatialKey, PointFeature[Double])): MutableArrayTile = {
    val (spatialKey, pointFeature) = tup
    val tileExtent = ld.mapTransform(spatialKey)
    val re = RasterExtent(tileExtent, tile)
    val result = tile.copy.asInstanceOf[MutableArrayTile]
    KernelStamper(result, kern).stampKernelDouble(re.mapToGrid(pointFeature.geom), pointFeature.data)
    result
  }

  import geotrellis.raster.mapalgebra.local.LocalTileBinaryOp

  object Adder extends LocalTileBinaryOp {
    def combine(z1: Int, z2: Int) = {
      if (isNoData(z1)) {
        z2
      } else if (isNoData(z2)) {
        z1
      } else {
        z1 + z2
      }
    }
    def combine(r1: Double, r2:Double) = {
      if (isNoData(r1)) {
        r2
      } else if (isNoData(r2)) {
        r1
      } else {
        r1 + r2
      }
    }
  }

  def sumTiles(t1: MutableArrayTile, t2: MutableArrayTile): MutableArrayTile = { 
    Adder(t1, t2).asInstanceOf[MutableArrayTile]
  }
```

Note that we require a custom Adder implementation because the built-in tile summation operator returns `NODATA` if either of the cells being added contain a `NODATA` value themselves.

Creating the desired result is then a matter of the following series of operations on `pointRdd`:
```scala
  val tileRdd: RDD[(SpatialKey, Tile)] =
    pointRdd
      .flatMap(ptfToSpatialKey)
      .mapPartitions({ partition =>
        partition.map { case (spatialKey, pointFeature) =>
          (spatialKey, (spatialKey, pointFeature))
        }
      }, preservesPartitioning = true)
      .aggregateByKey(ArrayTile.empty(DoubleCellType, ld.tileCols, ld.tileRows))(stampPointFeature, sumTiles)
      .mapValues{ tile: MutableArrayTile => tile.asInstanceOf[Tile] }
```
The `mapPartitions` operation simply applies a transformation to an RDD without triggering any kind of shuffling operation.
Here, it is necessary to make the SpatialKey available to `stampPointFeature` so that it can properly determine the pixel location in the corresponding tile.

We would be finished here, except that RDDs inside GeoTrellis are required to carry along a Metadata object that describes the context of the RDD.
This is created like so:
```scala
  val metadata = TileLayerMetadata(DoubleCellType,
                                   ld,
                                   ld.extent,
                                   LatLng,
                                   KeyBounds(SpatialKey(0,0),
                                             SpatialKey(ld.layoutCols-1,
                                                        ld.layoutRows-1)))
```
To combine the RDD and the metadata, we write `val resultRdd = ContextRDD(tileRdd, metadata)`.

This resulting RDD is essentially the object of interest, though it is possible to write `resultRDD.stitch` to produce a single merged tile.
In the general case, however, the RDD may cover an area so large and in sufficient resolution that the result of stitching would be too large for working memory.
In these sorts of applications, the usual work flow is to save the tiles off to one of the distributed back ends (Accumulo, S3, HDFS, etc.).
Tiles thus stored may then be used in further processing steps or be served to applications (e.g., web mapping applications).
If it is absolutely necessary, the individual tiles may be saved off as GeoTIFFs and stitched via an application like GDAL.

#### A Note on Running Example Code ####

To run the above test code, it is necessary to have a compatible environment.
Spark code may experience failures if run solely in the Scala interpreter, as accessed through SBT's `console` command.
One way to ensure proper execution is to run in `spark-shell`, a Scala environment which provides a SparkContext made available through the variable `sc`.
Another way is to compile the application into a JAR file using `sbt assembly`, and to use `spark-submit`.
This latter option is the preferred method for Spark applications, in general, but for the purposes of trying out the provided code samples, `spark-shell` is the more sensible choice.
The use of `spark-submit` is beyond the scope of this documentation, but many resources are available on the internet for learning this tool.

In either event, it will be necessary to install Spark in your local environment to run the code above.
Once that is done, you will need to clone the GeoTrellis repository from [Github](https://github.com/geotrellis/geotrellis).
From the root directory of that project, execute the provided `sbt` script.
Once SBT is loaded, the following commands can be executed:
```scala
  project spark-etl
  assembly
```
This packages the required class files into a JAR file.
Now, again from the GeoTrellis source tree root directory, issue the command
```bash
  spark-shell --jars spark-etl/target/scala-2.10/geotrellis-spark-etl-assembly-[version].jar
```
From the resulting interpreter prompt, perform the following imports:
```scala
  import geotrellis.raster._
  import geotrellis.vector._
  import geotrellis.proj4._
  import geotrellis.spark._
  import geotrellis.spark.util._
  import geotrellis.spark.tiling._
```
It should then be possible to input the example code from above (excluding the creation of a SparkContext) and get the desired result.

### A Note on Implementation ###

The procedures that we've been considering above have been implemented in GeoTrellis and are located in `raster/src/main/scala/geotrellis/raster/density/` and `spark/src/main/scala/geotrellis/spark/density`.
This final implementation is more complete than the simple version presented here, as it handles type conversion for different tile cell types and is augmented with convenience functions that are provided through the use of the `MethodExtensions` facility.
Briefly, method extensions allow for implicit conversion between `Traversable[PointFeature[Num]]` (where `Num` is either `Int` or `Double`) and a wrapper class which provides a method `kernelDensity: (Kernel, RasterExtent) => Tile`.
Thus, any traversable collection can be treated as if it possesses a `kernelDensity` method.
This pattern appears all throughout GeoTrellis, and provides some welcome syntactic sugar.

Furthermore, the final implementation is more flexible with regard to the type of data used.
Both the PointFeature parameter and the Tile CellType may be of integral or floating-point type.
See the code for details.





