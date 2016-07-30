# GeoTrellis

[![Build Status](https://api.travis-ci.org/geotrellis/geotrellis.svg)](http://travis-ci.org/geotrellis/geotrellis) [![Join the chat at https://gitter.im/geotrellis/geotrellis](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/geotrellis/geotrellis?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

*GeoTrellis* is a Scala library and framework that uses
Spark to work with raster data.  It is released under
the Apache 2 License.

GeoTrellis reads, writes, and operates on raster data
as fast as possible. It implements many
[Map Algebra](http://en.wikipedia.org/wiki/Map_algebra)
operations as well as vector to raster or raster to
vector operations.

GeoTrellis also provides tools to render rasters into
PNGs or to store metadata about raster files as JSON.
It aims to provide raster processing at web speeds (sub-second
or less) with RESTful endpoints as well as provide
fast batch processing of large raster data sets.

Please visit the **[project site](http://geotrellis.io)**
for more information as well as some interactive demos.


## Contact and Support

You can find more information and talk to developers
(let us know what you're working on!) at:

  - [Gitter](https://gitter.im/geotrellis/geotrellis)
  - [GeoTrellis mailing list](https://groups.google.com/group/geotrellis-user)

## Hello Raster

```scala
scala> import geotrellis.raster._
import geotrellis.raster._

scala> import geotrellis.raster.op.focal._
import geotrellis.raster.op.focal._

scala> val nd = NODATA
nd: Int = -2147483648

scala> val input = Array[Int](
     |         nd, 7, 1, 1, 3, 5, 9, 8, 2,
     |         9, 1, 1, 2, 2, 2, 4, 3, 5,
     |
     |         3, 8, 1, 3, 3, 3, 1, 2, 2,
     |         2, 4, 7, 1, nd, 1, 8, 4, 3)
input: Array[Int] = Array(-2147483648, 7, 1, 1, 3, 5, 9, 8, 2, 9, 1, 1, 2,
2, 2, 4, 3, 5, 3, 8, 1, 3, 3, 3, 1, 2, 2, 2, 4, 7, 1, -2147483648, 1, 8, 4, 3)

scala> val iat = IntArrayTile(input, 9, 4)  // 9 and 4 here specify columns and rows
iat: geotrellis.raster.IntArrayTile = IntArrayTile([I@278434d0,9,4)

// The asciiDraw method is mostly useful when you're working with small tiles
// which can be taken in at a glance
scala> iat.asciiDraw()
res0: String =
"    ND     7     1     1     3     5     9     8     2
     9     1     1     2     2     2     4     3     5
     3     8     1     3     3     3     1     2     2
     2     4     7     1    ND     1     8     4     3
"

scala> val focalNeighborhood = Square(1)  // a 3x3 square neighborhood
focalNeighborhood: geotrellis.raster.op.focal.Square =
 O  O  O
 O  O  O
 O  O  O

scala> val meanTile = iat.focalMean(focalNeighborhood)
meanTile: geotrellis.raster.Tile = DoubleArrayTile([D@7e31c125,9,4)

scala> meanTile.getDouble(0, 0)  // Should equal (1 + 7 + 9) / 3
res1: Double = 5.666666666666667
```

## GeoTrellis Features

This is a list of features contained in the GeoTrellis library. It is broken up by the subproject that contains the features.

#### geotrellis-proj4

- Represent a Coordinate Reference System (CRS) based on Ellipsoid, Datum, and Projection.
- Translate CRSs to and from proj4 string representations.
- Lookup CRS's based on EPSG and other codes.
- Transform `(x, y)` coordinates from one CRS to another.

#### geotrellis-vector

- Provides a scala idiomatic wrapper around JTS types: Point, Line (LineString in JTS), Polygon, MultiPoint, MultiLine (MultiLineString in JTS), MultiPolygon, GeometryCollection
- Methods for geometric operations supported in JTS, with results that provide a type-safe way to match over possible results of geometries.
- Provides a Feature type that is the composition of a geometry and a generic data type.
- Read and write geometries and features to and from GeoJSON.
- Read and write geometries to and from WKT and WKB.
- Reproject geometries between two CRSs.
- Geometric operations: Convex Hull, Densification, Simplification
- Perform Kriging interpolation on point values.
- Perform affine transformations of geometries

#### geotrellis-vector-testkit

- GeometryBuilder for building test geometries
- GeometryMatcher for scalatest unit tests, which aides in testing equality in geometries with an optional threshold.

#### geotrellis-raster

- Provides types to represent single- and multi-band rasters, supporting Bit, Byte, UByte, Short, UShort, Int, Float, and Double data, with either a constant NoData value (which improves performance) or a user defined NoData value.
- Treat a tile as a collection of values, by calling "map" and "foreach", along with floating point valued versions of those methods (separated out for performance).
- Combine raster data in generic ways.
- Render rasters via color ramps and color maps to PNG and JPG images.
- Read GeoTiffs with DEFLATE, LZW, and PackBits compression, including horizontal and floating point prediction for LZW and DEFLATE.
- Write GeoTiffs with DEFLATE or no compression.
- Reproject rasters from one CRS to another.
- Resample of raster data.
- Mask and Crop rasters.
- Split rasters into smaller tiles, and stitch tiles into larger rasters.
- Derive histograms from rasters in order to represent the distribution of values and create quantile breaks.
- Local Map Algebra operations: Abs, Acos, Add, And, Asin, Atan, Atan2, Ceil, Cos, Cosh, Defined, Divide, Equal, Floor, Greater, GreaterOrEqual, InverseMask, Less, LessOrEqual, Log, Majority, Mask, Max, MaxN, Mean, Min, MinN, Minority, Multiply, Negate, Not, Or, Pow, Round, Sin, Sinh, Sqrt, Subtract, Tan, Tanh, Undefined, Unequal, Variance, Variety, Xor, If
- Focal Map Algebra operations: Hillshade, Aspect, Slope, Convolve, Conway's Game of Life, Max, Mean, Median, Mode, Min, MoransI, StandardDeviation, Sum
- Zonal Map Algebra operations: ZonalHistogram, ZonalPercentage
- Operations that summarize raster data intersecting polygons: Min, Mean, Max, Sum.
- Cost distance operation based on a set of starting points and a friction raster.
- Hydrology operations: Accumulation, Fill, and FlowDirection.
- Rasterization of geometries and the ability to iterate over cell values covered by geometries.
- Vectorization of raster data.
- Kriging Interpolation of point data into rasters.
- Viewshed operation.
- RegionGroup operation.

#### geotrellis-raster-testkit

- Build test raster data.
- Assert raster data matches Array data or other rasters in scalatest.

#### geotrellis-spark

- Generic way to represent key value RDDs as layers, where the key represents a coordinate in space based on some uniform grid layout, optionally with a temporal component.
- Represent spatial or spatiotemporal raster data as an RDD of raster tiles.
- Generic architecture for saving/loading layers RDD data and metadata to/from various backends, using Spark's IO API with Space Filling Curve indexing to optimize storage retrieval (support for Hilbert curve and Z order curve SFCs). HDFS and local file system are supported backends by default, S3 and Accumulo are supported backends by the `geotrellis-s3` and `geotrellis-accumulo` projects, respectively.
- Query architecture that allows for simple querying of layer data by spatial or spatiotemporal bounds.
- Perform map algebra operations on layers of raster data, including all supported Map Algebra operations mentioned in the geotrellis-raster feature list.
- Perform seamless reprojection on raster layers, using neighboring tile information in the reprojection to avoid unwanted NoData cells.
- Pyramid up layers through zoom levels using various resampling methods.
- Types to reason about tiled raster layouts in various CRS's and schemes.
- Perform operations on raster RDD layers: crop, filter, join, mask, merge, partition, pyramid, render, resample, split, stitch, and tile.
- Polygonal summary over raster layers: Min, Mean, Max, Sum.
- Save spatially keyed RDDs of byte arrays to z/x/y files into HDFS or the local file system. Useful for saving PNGs off for use as map layers in web maps or for accessing GeoTiffs through z/x/y tile coordinates.
- Utilities around creating spark contexts for applications using GeoTrellis, including a Kryo registrator that registers most types.

#### geotrellis-spark-testkit

- Utility code to create test RDDs of raster data.
- Matching methods to test equality of RDDs of raster data in scalatest unit tests.

#### geotrellis-accumulo

- Save and load layers to and from Accumulo. Query large layers efficiently using the layer query API.

#### geotrellis-cassandra

- Save and load layers to and from Casandra. Query large layers efficiently using the layer query API.

#### geotrellis-hbase

- Save and load layers to and from HBase. Query large layers efficiently using the layer query API.

#### geotrellis-s3

- Save/load raster layers to/from the local filesystem or HDFS using Spark's IO API.
- Save spatially keyed RDDs of byte arrays to z/x/y files in S3. Useful for saving PNGs off for use as map layers in web maps.

#### geotrellis-etl

- Parse command line options for input and output of ETL (Extract, Transform, and Load) applications
- Utility methods that make ETL applications easier for the user to build.
- Work with input rasters from the local file system, HDFS, or S3
- Reproject input rasters using a per-tile reproject or a seamless reprojection that takes into account neighboring tiles.
- Transform input rasters into layers based on a ZXY layout scheme
- Save layers into Accumulo, S3, HDFS or the local file system.

#### geotrellis-shapefile

- Read geometry and feature data from shapefiles into GeoTrellis types using GeoTools.

#### geotrellis-slick

- Save and load geometry and feature data to and from PostGIS using the slick scala database library.
- Perform PostGIS `ST_` operations in PostGIS through scala.

## Documentation

- Further examples and documentation of GeoTrellis use-cases can be found in the [docs/](./docs) folder
- *Scaladocs* for the latest version of the project can be found here:

[http://geotrellis.github.com/scaladocs/latest/#geotrellis.package](http://geotrellis.github.com/scaladocs/latest/#geotrellis.package)


## Contributors

 - Josh Marcus
 - Erik Osheim
 - Rob Emanuele
 - Adam Hinz
 - Michael Tedeschi
 - Robert Cheetham
 - Justin Walgran
 - Eric J. Christeson
 - Ameet Kini
 - Mark Landry
 - Walt Chen
 - Eugene Cheipesh

## Contributing

Feedback and contributions to the project, no matter what kind,
are always very welcome. A CLA is required for contribution, see
[contributing.md](./contributing.md) for more information. Please
refer to the [Scala style guide](http://docs.scala-lang.org/style/)
for formatting patches to the codebase.
