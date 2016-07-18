
The long awaited GeoTrellis 0.10 release is here!

It’s been a while since the 0.9 release of GeoTrellis, and there are many significant changes and improvements in this release. GeoTrellis has become an expansive suite of modular components that aide users in the building of geospatial application in Scala, and as always we’ve focused specifically on high performance and distributed computing. This is the first official release that supports working with Apache Spark, and we are very pleased with the results that have come out of the decision to support Spark as our main distributed processing engine. Those of you who have been tuned in for a while know we started with a custom built processing engine based on Akka actors; this original execution engine still exists in 0.10 but is in a deprecated state in the geotrellis-engine subproject. Along with upgrading GeoTrellis to support Spark and handle arbitrarily-sized raster data sets, we’ve been making improvements and additions to core functionality, including adding vector and projection support.

It’s been long enough that release notes, stating what has changed since 0.9, would be quite unwieldy. Instead I put together a list of features that GeoTrellis 0.10 supports. This is included in the README on the GeoTrellis Github, but I will put them here as well. It is organized by subproject, with more basic and core subprojects higher in the list, and the subprojects that rely on that core functionality later in the list, along with a high level description of each subproject.


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

Save and load layers to and from Casandra. Query large layers efficiently using the layer query API.

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
