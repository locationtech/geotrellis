GeoTrellis Module Hierarchy
***************************

This is a full list of all GeoTrellis modules. While there is some
interdependence between them, you can depend on as many (or as few) of
them as you want in your ``build.sbt``.

geotrellis-accumulo
-------------------

Implements ``geotrellis.store`` types for `Apache Accumulo <https://accumulo.apache.org/>`__.

*Provides:* ``geotrellis.store.accumulo.*``

-  Save and load layers to and from Accumulo. Query large layers
   efficiently using the layer query API.

geotrellis-accumulo-spark
-------------------------

Implements ``geotrellis.spark.store`` types for `Apache Accumulo <https://accumulo.apache.org/>`__,
extending ``geotrellis-accumulo``.

*Provides:* ``geotrellis.spark.store.accumulo.*``

-  Save and load layers to and from Accumulo within a Spark Context using RDDs.
-  Supoort Accumulo backend for TileLayerRDDs.

geotrellis-cassandra
-------------------

Implements ``geotrellis.store`` types for `Apache Cassandra <http://cassandra.apache.org/>`__.

*Provides:* ``geotrellis.store.cassandra.*``

-  Save and load layers to and from Cassandra. Query large layers
   efficiently using the layer query API.

geotrellis-cassandra-spark
-------------------------

Implements ``geotrellis.spark.store`` types for `Apache Cassandra <https://cassandra.apache.org/>`__,
extending ``geotrellis-cassandra``.

*Provides:* ``geotrellis.spark.store.cassandra.*``

-  Save and load layers to and from Cassandra within a Spark Context using RDDs.
-  Supoort Accumulo backend for TileLayerRDDs.

geotrellis-gdal
-------------------------

Implements GeoTrellis `GDAL <https://gdal.org/>`__ support.

*Provides:* ``geotrellis.raster.gdal.*``

-  Implements GDALRasterSources to read, reproject, resample, convert rasters.
   Performs all transformations via GDALWarp.

geotrellis-gdal-spark
-------------------------

Contains geotrellis.raster.gdal.* integration tests for Spark.

geotrellis-geomesa
------------------

*Experimental.* GeoTrellis compatibility for the distributed feature
store `GeoMesa <http://www.geomesa.org/>`__.

*Provides:* ``geotrellis.geomesa.geotools.*``
*Provides:* ``geotrellis.spark.store.geomesa.*``

-  Save and load ``RDD``\ s of features to and from GeoMesa.

geotrellis-geotools
-------------------

*Provides:* ``geotrellis.geotools.*``

-  Conversion functions between GeoTrellis, OpenGIS and GeoTools ``Features``.

*Provides:* ``geotrellis.geotools.*``

geotrellis-geowave
------------------

*Experimental.* GeoTrellis compatibility for the distributed feature
store `GeoWave <https://github.com/ngageoint/geowave>`__.

*Provides:* ``geotrellis.spark.io.geowave.*``

-  Save and load ``RDD``\ s of features to and from GeoWave.

geotrellis-hbase
-------------------

Implements ``geotrellis.store`` types for `Apache HBase <http://hbase.apache.org/>`__.

*Provides:* ``geotrellis.store.hbase.*``

-  Save and load layers to and from HBase. Query large layers
   efficiently using the layer query API.

geotrellis-hbase-spark
-------------------------

Implements ``geotrellis.spark.store`` types for `Apache hbase <https://hbase.apache.org/>`__,
extending ``geotrellis-hbase``.

*Provides:* ``geotrellis.spark.store.hbase.*``

-  Save and load layers to and from HBase within a Spark Context using RDDs.
-  Supoort Accumulo backend for TileLayerRDDs.

geotrellis-layer
----------------

Datatypes to describe Layers (sets of spatially referenced rasters).

*Provides:* ``geotrellis.layer.*``

-  Generic way to represent key value ``Seq``s as layers, where the key
   represents a coordinate in space based on some uniform grid layout,
   optionally with a temporal component.
-  Contains data types to describe ``LayoutSchemes`` and ``LayoutDefinitions``,
   ``KeyBounds``, layer key types (``SpatialKey``, ``SpaceTimeKey``) and ``TileLayerMetadata``
   layer metadata type.
-  Implements ``SpaceTimeKey`` ``collection layer`` projection to the ``SpatialKey`` space.
-  MapAlgebra (focal and local) for ``collection layers``.
-  Mask and Stitch operations for ``collection layers``.
-  Implements tiling for ``RasterSources``.

geotrellis-macros
-----------------

The intention of this package is to keep API both performant and expressive enough.

*Provides:* ``geotrellis.macros.*``

-  Contains inline macro implementations for ``Tile`` ``NoData``, ``foreach``, ``map`` and some
   type conversions.

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
-  Polygonal Summary operations that summarize raster data intersecting polygons: Min,
   Mean, Max, Sum, Histogram.
-  Cost distance operation based on a set of starting points and a
   friction raster.
-  Hydrology operations: Accumulation, Fill, and FlowDirection.
-  Rasterization of geometries and the ability to iterate over cell
   values covered by geometries.
-  Vectorization of raster data.
-  Kriging Interpolation of point data into rasters.
-  Viewshed operation.
-  RegionGroup operation.
-  Kernel density estimation.
-  Raster histogram equalization and matching methods.
-  Delaunay triangulation rasterizer.
-  Provides an abstract, higher order API for reading ``RasterSources``
   from different sources. ``RasterSource`` is an abstraction over I/O implementations.
   Other ``GeoTrellis`` packages provide concrete ``RasterSource`` implementations,
   such as ``GDALRasterSource`` in a ``geotrellis.raster.gdal`` package.
-  Implements lazy RasterSource transformation operations:
   reprojection, resampling and cellType conversion.

geotrellis-raster-testkit
-------------------------

Integration tests for ``geotrellis-raster``.

-  Build test raster data.
-  Assert raster data matches Array data or other rasters in scalatest.

geotrellis-s3
-------------

Implements the ``geotrellis.store`` types for the AWS Simple Storage Service (S3) backend.

Allows the use of `Amazon S3 <https://aws.amazon.com/s3/>`__ as a Tile
layer backend.

*Provides:* ``geotrellis.store.s3.*``

-  Save/load raster layers to/from S3
-  Save/load Cloud Optimized GeoTiffs (COGs) to/from S3

geotrellis-s3-spark
-------------------

Implements ``geotrellis.store`` and ``geotrellis.spark`` types for interoperability between
GeoTrellis, Spark and S3.

*Provides:* ``geotrellis.spark.store.s3.*``

-  Save/load Spark RDD Tile layers to/from S3
-  Support S3 operations on GeoTiff, COG and Slippy tiles
-  Use SaveToS3 to save pyramided image and vector tile layers in X/Y/Z format

geotrellis-shapefile
--------------------

*Provides:* ``geotrellis.shapefile.*``

-  Read geometry and feature data from shapefiles into GeoTrellis types
   using GeoTools.

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
-  Implements GeoTrellis ``COGLayer`` creation, persistence and query mechanisms.

geotrellis-spark-pipeline
-------------------------

Pipelines are the operative construct in GeoTrellis,
the original idea was taken from `PDAL <https://pdal.io/pipeline.html>`__.
Pipelines represent a set of instructions rather than a simple ETL process:
how to read data, transform (process), write it. The result of the Pipeline
should not always be writing, it can also be some intermediate transformation result,
or just a raw data.

*Provides:* ``geotrellis.spark.pipeline.*``

-  Provides a JSON DSL that represents a set of instructions performed on some data source.
-  Provides a Scala DSL that abstracts over GeoTrellis pipeline operations. It also allows
   users to avoid manually writing the JSON DSL.
-  Allows reads (from local file system, s3, hdfs, etc), transformations (tile-to-layout,
   reproject, pyramid) and writes (all supported GeoTrellis stores).

geotrellis-spark-testkit
------------------------

Integration tests for ``geotrellis-spark``.

-  Utility code to create test RDDs of raster data.
-  Matching methods to test equality of RDDs of raster data in scalatest
   unit tests.

geotrellis-store
----------------

Types and interfaces for interacting with a number of different storage backends in an abstract way.

In older versions of GeoTrellis, ``store`` implementations were referred to as ``backends``.

*Provides:* ``geotrellis.store.*``

-  Contains interfaces for ``LayerReaders``, ``LayerWriters`` and ``ValueReaders``.
-  Avro ``Tile`` codecs.
-  Local file system and HDFS ``COG`` and ``GeoTrellis`` ``Value`` and ``Collection`` readers implementation.
-  Indexing strategies implementation: ZCurve and HilbertCurve.
-  GeoTrellisRasterSources that implement access to GeoTrellis layers through the new API.

geotrellis-util
---------------

Plumbing for other GeoTrellis modules.

*Provides:* ``geotrellis.util.*``

-  Constants
-  Data structures missing from Scala, such as BTree
-  Haversine implementation
-  Lenses
-  RangeReaderProvider for reading contiguous subsets of data from a source
  - Implementations for FileRangeReader and HttpRangeReader

geotrellis-vector
-----------------

Types and algorithms for processing Vector data.

*Provides:* ``geotrellis.vector.*``

-  Provides idiomatic helpers for the JTS types: Point, LineString,
   Polygon, MultiPoint, MultiLineString, MultiPolygon, GeometryCollection
-  Methods for geometric operations supported in JTS, with results that
   provide a type-safe way to match over possible results of geometries.
-  Provides a Feature type that is the composition of an id, geometry and a
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

*Experimental.* A full `Mapbox VectorTile <https://www.mapbox.com/vector-tiles/>`__ codec.

*Provides:* ``geotrellis.vectortile.*``

-  Lazy decoding
-  Read/write ``VectorTile`` tile layers from any tile backend
