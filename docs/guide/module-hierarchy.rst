GeoTrellis Module Hierarchy
***************************

This is a full list of all GeoTrellis modules. While there is some
interdependence between them, you can depend on as many (or as few) of
them as you want in your ``build.sbt``.

geotrellis-accumulo
-------------------

Allows the use of `Apache Accumulo <https://accumulo.apache.org/>`__ as
a Tile layer backend.

*Provides:* ``geotrellis.spark.io.accumulo.*``

-  Save and load layers to and from Accumulo. Query large layers
   efficiently using the layer query API.

geotrellis-cassandra
--------------------

Allows the use of `Apache Cassandra <http://cassandra.apache.org/>`__ as
a Tile layer backend.

*Provides:* ``geotrellis.spark.io.cassandra.*``

-  Save and load layers to and from Cassandra. Query large layers
   efficiently using the layer query API.

geotrellis-etl
--------------

A command-line tool for streamlining the ingest process.

*Provides:* ``geotrellis.spark.etl.*``

-  Parse command line options for input and output of ETL (Extract,
   Transform, and Load) applications
-  Utility methods that make ETL applications easier for the user to
   build.
-  Work with input rasters from the local file system, HDFS, or S3
-  Reproject input rasters using a per-tile reproject or a seamless
   reprojection that takes into account neighboring tiles.
-  Transform input rasters into layers based on a ZXY layout scheme
-  Save layers into Accumulo, S3, HDFS or the local file system.

geotrellis-geomesa
------------------

*Experimental.* GeoTrellis compatibility for the distributed feature
store `GeoMesa <http://www.geomesa.org/>`__.

*Provides:* ``geotrellis.spark.io.geomesa.*``

-  Save and load ``RDD``\ s of features to and from GeoMesa.

geotrellis-hbase
----------------

Allows the use of `Apache HBase <http://hbase.apache.org/>`__ as a Tile
layer backend.

*Provides:* ``geotrellis.spark.io.hbase.*``

-  Save and load layers to and from HBase. Query large layers
   efficiently using the layer query API.

geotrellis-proj4
----------------

*Provides:* ``geotrellis.proj4.*``, ``org.osgeo.proj4.*`` (Java)

-  Represent a Coordinate Reference System (CRS) based on Ellipsoid,
   Datum, and Projection.
-  Translate CRSs to and from proj4 string representations.
-  Lookup CRS's based on EPSG and other codes.
-  Transform ``(x, y)`` coordinates from one CRS to another.

geotrellis-raster
-----------------

Types and algorithms for Raster processing.

*Provides:* ``geotrellis.raster.*``

-  Provides types to represent single- and multi-band rasters,
   supporting Bit, Byte, UByte, Short, UShort, Int, Float, and Double
   data, with either a constant NoData value (which improves
   performance) or a user defined NoData value.
-  Treat a tile as a collection of values, by calling "map" and
   "foreach", along with floating point valued versions of those methods
   (separated out for performance).
-  Combine raster data in generic ways.
-  Render rasters via color ramps and color maps to PNG and JPG images.
-  Read GeoTiffs with DEFLATE, LZW, and PackBits compression, including
   horizontal and floating point prediction for LZW and DEFLATE.
-  Write GeoTiffs with DEFLATE or no compression.
-  Reproject rasters from one CRS to another.
-  Resample of raster data.
-  Mask and Crop rasters.
-  Split rasters into smaller tiles, and stitch tiles into larger
   rasters.
-  Derive histograms from rasters in order to represent the distribution
   of values and create quantile breaks.
-  Local Map Algebra operations: Abs, Acos, Add, And, Asin, Atan, Atan2,
   Ceil, Cos, Cosh, Defined, Divide, Equal, Floor, Greater,
   GreaterOrEqual, InverseMask, Less, LessOrEqual, Log, Majority, Mask,
   Max, MaxN, Mean, Min, MinN, Minority, Multiply, Negate, Not, Or, Pow,
   Round, Sin, Sinh, Sqrt, Subtract, Tan, Tanh, Undefined, Unequal,
   Variance, Variety, Xor, If
-  Focal Map Algebra operations: Hillshade, Aspect, Slope, Convolve,
   Conway's Game of Life, Max, Mean, Median, Mode, Min, MoransI,
   StandardDeviation, Sum
-  Zonal Map Algebra operations: ZonalHistogram, ZonalPercentage
-  Operations that summarize raster data intersecting polygons: Min,
   Mean, Max, Sum.
-  Cost distance operation based on a set of starting points and a
   friction raster.
-  Hydrology operations: Accumulation, Fill, and FlowDirection.
-  Rasterization of geometries and the ability to iterate over cell
   values covered by geometries.
-  Vectorization of raster data.
-  Kriging Interpolation of point data into rasters.
-  Viewshed operation.
-  RegionGroup operation.

geotrellis-raster-testkit
-------------------------

Integration tests for ``geotrellis-raster``.

-  Build test raster data.
-  Assert raster data matches Array data or other rasters in scalatest.

geotrellis-s3
-------------

Allows the use of `Amazon S3 <https://aws.amazon.com/s3/>`__ as a Tile
layer backend.

*Provides:* ``geotrellis.spark.io.s3.*``

-  Save/load raster layers to/from the local filesystem or HDFS using
   Spark's IO API.
-  Save spatially keyed RDDs of byte arrays to z/x/y files in S3. Useful
   for saving PNGs off for use as map layers in web maps.

geotrellis-shapefile
--------------------

*Provides:* ``geotrellis.shapefile.*``

-  Read geometry and feature data from shapefiles into GeoTrellis types
   using GeoTools.

geotrellis-slick
----------------

Adds PostGis support for `Slick <https://github.com/slick/slick>`__ use
with GeoTrellis.

*Provides:* ``geotrellis.slick.*``

-  Save and load geometry and feature data to and from PostGIS using the
   slick scala database library.
-  Perform PostGIS ``ST_`` operations in PostGIS through scala.

geotrellis-spark
----------------

Tile layer algorithms powered by `Apache
Spark <http://spark.apache.org/>`__.

*Provides:* ``geotrellis.spark.*``

-  Generic way to represent key value RDDs as layers, where the key
   represents a coordinate in space based on some uniform grid layout,
   optionally with a temporal component.
-  Represent spatial or spatiotemporal raster data as an RDD of raster
   tiles.
-  Generic architecture for saving/loading layers RDD data and metadata
   to/from various backends, using Spark's IO API with Space Filling
   Curve indexing to optimize storage retrieval (support for Hilbert
   curve and Z order curve SFCs). HDFS and local file system are
   supported backends by default, S3 and Accumulo are supported backends
   by the ``geotrellis-s3`` and ``geotrellis-accumulo`` projects,
   respectively.
-  Query architecture that allows for simple querying of layer data by
   spatial or spatiotemporal bounds.
-  Perform map algebra operations on layers of raster data, including
   all supported Map Algebra operations mentioned in the
   geotrellis-raster feature list.
-  Perform seamless reprojection on raster layers, using neighboring
   tile information in the reprojection to avoid unwanted NoData cells.
-  Pyramid up layers through zoom levels using various resampling
   methods.
-  Types to reason about tiled raster layouts in various CRS's and
   schemes.
-  Perform operations on raster RDD layers: crop, filter, join, mask,
   merge, partition, pyramid, render, resample, split, stitch, and tile.
-  Polygonal summary over raster layers: Min, Mean, Max, Sum.
-  Save spatially keyed RDDs of byte arrays to z/x/y files into HDFS or
   the local file system. Useful for saving PNGs off for use as map
   layers in web maps or for accessing GeoTiffs through z/x/y tile
   coordinates.
-  Utilities around creating spark contexts for applications using
   GeoTrellis, including a Kryo registrator that registers most types.

geotrellis-spark-testkit
------------------------

Integration tests for ``geotrellis-spark``.

-  Utility code to create test RDDs of raster data.
-  Matching methods to test equality of RDDs of raster data in scalatest
   unit tests.

geotrellis-geotools
-------------------

*Provides:* ``geotrellis.geotools.*``

geotrellis-vector
-----------------

Types and algorithms for processing Vector data.

*Provides:* ``geotrellis.vector.*``

-  Provides a scala idiomatic wrapper around JTS types: Point, Line
   (LineString in JTS), Polygon, MultiPoint, MultiLine (MultiLineString
   in JTS), MultiPolygon, GeometryCollection
-  Methods for geometric operations supported in JTS, with results that
   provide a type-safe way to match over possible results of geometries.
-  Provides a Feature type that is the composition of a geometry and a
   generic data type.
-  Read and write geometries and features to and from GeoJSON.
-  Read and write geometries to and from WKT and WKB.
-  Reproject geometries between two CRSs.
-  Geometric operations: Convex Hull, Densification, Simplification
-  Perform Kriging interpolation on point values.
-  Perform affine transformations of geometries

geotrellis-vector-testkit
-------------------------

Integration tests for ``geotrellis-vector``.

-  GeometryBuilder for building test geometries
-  GeometryMatcher for scalatest unit tests, which aides in testing
   equality in geometries with an optional threshold.

geotrellis-vectortile
---------------------

*Experimental.* A full `Mapbox
VectorTile <https://www.mapbox.com/vector-tiles/>`__ codec.

*Provides:* ``geotrellis.vectortile.*``

-  Lazy decoding
-  Read/write ``VectorTile`` tile layers from any tile backend

geotrellis-util
---------------

Plumbing for other GeoTrellis modules.

*Provides:* ``geotrellis.util.*``

-  Data structures missing from Scala
-  Lenses
-  Constants

geotrellis-geowave
------------------

*Experimental.* GeoTrellis compatibility for the distributed feature
store `GeoWave <https://github.com/ngageoint/geowave>`__.

*Provides:* ``geotrellis.spark.io.geowave.*``

-  Save and load ``RDD``\ s of features to and from GeoWave.
