1.0.0
------

<h3>Major Features</h3>

- GeoTools support
    - Add Support for GeoTools SimpleFeature [#1495](https://github.com/locationtech/geotrellis/pull/1495)
    - Conversions between GeoTools GridCoverage2D and GeoTrellis Raster types [#1502](https://github.com/locationtech/geotrellis/pull/1502)
- Streaming GeoTiff reading [#1559](https://github.com/locationtech/geotrellis/pull/1559)
- Windowed GeoTiff ingests into GeoTrellis layers, allowing users to ingest
large GeoTiffs [#1763](https://github.com/locationtech/geotrellis/pull/1763)
    - Reading TiffTags via MappedByteBuffer [#1541](https://github.com/locationtech/geotrellis/pull/1541)
    - Cropped Windowed GeoTiff Reading [#1559](https://github.com/locationtech/geotrellis/pull/1559)
    - Added documentation to the GeoTiff* files [#1560](https://github.com/locationtech/geotrellis/pull/1560)
    - Windowed GeoTiff Docs [#1616](https://github.com/locationtech/geotrellis/pull/1616)
- GeoWave Raster/Vector support (experimental)
    - Create GeoWave Subproject [#1542](https://github.com/locationtech/geotrellis/pull/1542)
    - Add vector capabilities to GeoWave support [#1581](https://github.com/locationtech/geotrellis/pull/1581)
    - Fix GeoWave Tests [#1665](https://github.com/locationtech/geotrellis/pull/1665)
- GeoMesa Vector support (experimental)
    - Create GeoMesa suproject [#1621](https://github.com/locationtech/geotrellis/pull/1621)
- Moved to a JSON-configuration ETL process
    - ETL Refactor [#1553](https://github.com/locationtech/geotrellis/pull/1553)
    - ETL Improvements and other issues fixes [#1647](https://github.com/locationtech/geotrellis/pull/1647)
- Vector Tile reading and writing, file-based and as GeoTrellis layers in
RDDs. [#1622](https://github.com/locationtech/geotrellis/pull/1622)
- File Backends
    - Cassandra support [#1452](https://github.com/locationtech/geotrellis/pull/1452)
    - HBase support [#1586](https://github.com/locationtech/geotrellis/pull/1586)
- Collections API [#1606](https://github.com/locationtech/geotrellis/pull/1606)
    - Collections polygonal summary functions [#1614](https://github.com/locationtech/geotrellis/pull/1614)
    - Collections mapalgebra focal functions [#1619](https://github.com/locationtech/geotrellis/pull/1619)
- Add `TileFeature` Type [#1429](https://github.com/locationtech/geotrellis/pull/1429)
- Added Focal calculation target type [#1601](https://github.com/locationtech/geotrellis/pull/1601)
- Triangulation
    - Voronoi diagrams and Delaunay triangulations [#1545](https://github.com/locationtech/geotrellis/pull/1545), [#1699](https://github.com/locationtech/geotrellis/pull/1699)
    - Conforming Delaunay Triangulation [#1848](https://github.com/locationtech/geotrellis/pull/1848)
- Euclidean distance tiles [#1552](https://github.com/locationtech/geotrellis/pull/1552)
- Spark, Scala and Java version version support
    - Move to Spark 2; Scala 2.10 deprecation [#1628](https://github.com/locationtech/geotrellis/pull/1628)
    - Java 7 deprecation [#1640](https://github.com/locationtech/geotrellis/pull/1640)
- Color correction features:
    - Histogram Equalization [#1668](https://github.com/locationtech/geotrellis/pull/1668)
    - Sigmoidal Contrast [#1681](https://github.com/locationtech/geotrellis/pull/1681)
    - Histogram matching [#1769](https://github.com/locationtech/geotrellis/pull/1769)
- `CollectNeighbors` feature, allowing users to group arbitrary values by
the neighbor keys according to their SpatialComponent
[#1860](https://github.com/locationtech/geotrellis/pull/1860)
- **Documentation:** We moved to ReadTheDocs, and put a lot of work into making
our docs significantly better. [See them here.](http://geotrellis.readthedocs.io/en/1.0/)

<h3>Minor Additions</h3>

- Documentation improvements
    - Quickstart
    - Examples
        - Added example for translating from `SpaceTimeKey` to `SpatialKey` [#1549](https://github.com/locationtech/geotrellis/pull/1549)
        - doc-examples subproject; example for tiling to GeoTiff [#1564](https://github.com/locationtech/geotrellis/pull/1564)
        - Added example for focal operation on multiband layer. [#1577](https://github.com/locationtech/geotrellis/pull/1577)
        - Projections, Extents, and Layout Definitions doc [#1608](https://github.com/locationtech/geotrellis/pull/1608)
        - Added example of turning a list of features into GeoJson [#1609](https://github.com/locationtech/geotrellis/pull/1609)
        - Example: `ShardingKeyIndex[K]` [#1633](https://github.com/locationtech/geotrellis/pull/1633)
        - Example: `VoxelKey` [#1639](https://github.com/locationtech/geotrellis/pull/1639)
 - Introduce ADR concept
    - ADR: HDFS Raster Layers [#1582](https://github.com/locationtech/geotrellis/pull/1582)
    - [ADR] Readers / Writers Multithreading [#1613](https://github.com/locationtech/geotrellis/pull/1613)
- Fixes
    - Fixed some markdown docs [#1625](https://github.com/locationtech/geotrellis/pull/1625)
    - `parseGeoJson` lives in geotrellis.vector.io [#1649](https://github.com/locationtech/geotrellis/pull/1649)
- Parallelize reads for S3, File, and Cassandra backends
[#1607](https://github.com/locationtech/geotrellis/pull/1607)
- Kernel Density in Spark
- k-Nearest Neighbors
- Updated slick
- Added GeoTiff read/write support of TIFFTAG_PHOTOMETRIC via
`GeoTiffOptions`.
[#1667](https://github.com/locationtech/geotrellis/pull/1667)
- Added ability to read/write color tables for GeoTIFFs encoded with palette
photometric interpretation
[#1802](https://github.com/locationtech/geotrellis/pull/1802)
- Added `ColorMap` to String conversion [#1512](https://github.com/locationtech/geotrellis/pull/1512)
- Add split by cols/rows to SplitMethods [#1538](https://github.com/locationtech/geotrellis/pull/1538)
- Improved HDFS support [#1556](https://github.com/locationtech/geotrellis/pull/1556)
- Added Vector Join operation for Spark [#1610](https://github.com/locationtech/geotrellis/pull/1610)
- Added Histograms Over Fractions of RDDs of Tiles
[#1692](https://github.com/locationtech/geotrellis/pull/1692)
- Add `interpretAs` and `withNoData` methods to Tile
[#1702](https://github.com/locationtech/geotrellis/pull/1702)
- Changed GeoTiff reader to handle BigTiff [#1753](https://github.com/locationtech/geotrellis/pull/1753)
- Added `BreakMap` for reclassification based on range values.
[#1760](https://github.com/locationtech/geotrellis/pull/1760)
- Allow custom save actions on ETL [#1764](https://github.com/locationtech/geotrellis/pull/1764)
- Multiband histogram methods [#1784](https://github.com/locationtech/geotrellis/pull/1784)
- `DelayedConvert` feature, allowing users to delay conversions on tiles until
a map or combine operation, so that tiles are not iterated over
unnecessarily [#1797](https://github.com/locationtech/geotrellis/pull/1797)
- Add convenience overloads to GeoTiff companion object
[#1840](https://github.com/locationtech/geotrellis/pull/1840)

<h3>Fixes / Optimizations</h3>

- Fixed GeoTiff bug in reading NoData value if len = 4
[#1490](https://github.com/locationtech/geotrellis/pull/1490)
- Add detail to avro exception message [#1505](https://github.com/locationtech/geotrellis/pull/1505)
- Fix: The toSpatial Method gives metadata of type TileLayerMetadata[SpaceTimeKey]
    - Custom `Functor` Typeclass [#1643](https://github.com/locationtech/geotrellis/pull/1643)
- Allow `Intersects(polygon: Polygon)` in layer query
[#1644](https://github.com/locationtech/geotrellis/pull/1644)
- Optimize `ColorMap` [#1648](https://github.com/locationtech/geotrellis/pull/1648)
- Make regex for s3 URLs handle s3/s3a/s3n [#1652](https://github.com/locationtech/geotrellis/pull/1652)
- Fixed metadata handling on surface calculation for tile layer RDDs
[#1684](https://github.com/locationtech/geotrellis/pull/1684)
- Fixed reading GeoJson with 3d values [#1704](https://github.com/locationtech/geotrellis/pull/1704)
- Fix to Bicubic Interpolation [#1708](https://github.com/locationtech/geotrellis/pull/1708)
- Fixed: Band tags with values of length > 31 have additional white space
added to them [#1756](https://github.com/locationtech/geotrellis/pull/1756)
- Fixed NoData bug in tile merging logic [#1793](https://github.com/locationtech/geotrellis/pull/1793)
- Fixed Non-Point Pixel + Partial Cell Rasterizer Bug
[#1804](https://github.com/locationtech/geotrellis/pull/1804)

<h3>New committers</h3>

- metasim
- lokifacio
- aeffrig
- jpolchlo
- jbouffard
- vsimko
- longcmu
- miafg

0.10.3
------

- [PR #1611](https://github.com/geotrellis/geotrellis/pull/1611) Any `RDD`
of `Tile`s can utilize Polygonal Summary methods. (@fosskers)
- [PR #1573](https://github.com/geotrellis/geotrellis/pull/1573) New
`foreach` for `MultibandTile` which maps over each band at once. (@hjaekel)
- [PR #1600](https://github.com/geotrellis/geotrellis/pull/1600) New
`mapBands` method to map more cleanly over the bands of a `MultibandTile`.
(@lossyrob)

0.10.2
------

- [PR #1561](https://github.com/geotrellis/geotrellis/pull/1561) Fix to
polygon sequence union, account that it can result in NoResult. (@lossyrob)
- [PR #1585](https://github.com/geotrellis/geotrellis/pull/1585) Removed
warnings; add proper subtyping to GetComponent and SetComponent identity
implicits; fix jai travis breakage. (@lossyrob)
- [PR #1569](https://github.com/geotrellis/geotrellis/pull/1569) Moved
RDDLayoutMergeMethods functionality to object. (@lossyrob)
- [PR #1494](https://github.com/geotrellis/geotrellis/pull/1494) Add ETL
option to specify upper zoom limit for raster layer ingestion (@mbertrand)
- [PR #1571](https://github.com/geotrellis/geotrellis/pull/1571) Fix scallop
upgrade issue in spark-etl (@pomadchin)
- [PR #1543](https://github.com/geotrellis/geotrellis/pull/1543) Fix to
Hadoop LayerMover (@pomadchin)

Special thanks to new contributor @mbertrand!

0.10.1
------

- PR #1451 Optimize reading from compressed Bit geotiffs (@shiraeeshi)
- PR #1454 Fix issues with IDW interpolation (@lokifacio)
- PR #1457 Store FastMapHistogram counts as longs (@jpolchlo)
- PR #1460 Fixes to user defined float/double CellType parsing (@echeipesh)
- PR #1461 Pass resampling method argument to merge in CutTiles (@lossyrob)
- PR #1466 Handle Special Characters in proj4j (@jamesmcclain)
- PR #1468 Fix nodata values in codecs (@shiraeeshi)
- PR #1472 Fix typo in MultibandIngest.scala (@timothymschier)
- PR #1478 Fix month and year calculations (@shiraeeshi)
- PR #1483 Fix Rasterizer Bug (@jamesmcclain)
- PR #1485 Upgrade dependencies as part of our LocationTech CQ process (@lossyrob)
- PR #1487 Handle entire layers of NODATA (@fosskers)
- PR #1493 Added support for int32raw cell types in CellType.fromString (@jpolchlo)
- PR #1496 Update slick (@adamkozuch, @moradology)
- PR #1498 Add ability to specify number of streaming buckets (@moradology)
- PR #1500 Add logic to ensure use of minval/avoid repetition of breaks (@moradology)
- PR #1501 SparkContext temporal GeoTiff format args (@echeipesh)
- PR #1510 Remove dep on cellType when specifying layoutExtent (@fosskers)
- PR #1529 LayerUpdater fix (@pomadchin)

Special thanks to new contributors @fosskers, @adamkozuch, @jpolchlo,
@shiraeeshi, @lokifacio!

0.10.0
------

The long awaited GeoTrellis 0.10 release is here!

It’s been a while since the 0.9 release of GeoTrellis, and there are many
significant changes and improvements in this release. GeoTrellis has become
an expansive suite of modular components that aide users in the building of
geospatial application in Scala, and as always we’ve focused specifically on
high performance and distributed computing. This is the first official
release that supports working with Apache Spark, and we are very pleased
with the results that have come out of the decision to support Spark as our
main distributed processing engine. Those of you who have been tuned in for
a while know we started with a custom built processing engine based on Akka
actors; this original execution engine still exists in 0.10 but is in a
deprecated state in the geotrellis-engine subproject. Along with upgrading
GeoTrellis to support Spark and handle arbitrarily-sized raster data sets,
we’ve been making improvements and additions to core functionality,
including adding vector and projection support.

It’s been long enough that release notes, stating what has changed since
0.9, would be quite unwieldy. Instead I put together a list of features that
GeoTrellis 0.10 supports. This is included in the README on the GeoTrellis
Github, but I will put them here as well. It is organized by subproject,
with more basic and core subprojects higher in the list, and the subprojects
that rely on that core functionality later in the list, along with a high
level description of each subproject.

**geotrellis-proj4**

- Represent a Coordinate Reference System (CRS) based on Ellipsoid, Datum, and Projection.
- Translate CRSs to and from proj4 string representations.
- Lookup CRS's based on EPSG and other codes.
- Transform `(x, y)` coordinates from one CRS to another.

**geotrellis-vector**

- Provides a scala idiomatic wrapper around JTS types: Point, Line (LineString in JTS), Polygon, MultiPoint, MultiLine (MultiLineString in JTS), MultiPolygon, GeometryCollection
- Methods for geometric operations supported in JTS, with results that provide a type-safe way to match over possible results of geometries.
- Provides a Feature type that is the composition of a geometry and a generic data type.
- Read and write geometries and features to and from GeoJSON.
- Read and write geometries to and from WKT and WKB.
- Reproject geometries between two CRSs.
- Geometric operations: Convex Hull, Densification, Simplification
- Perform Kriging interpolation on point values.
- Perform affine transformations of geometries

**geotrellis-vector-testkit**

- GeometryBuilder for building test geometries
- GeometryMatcher for scalatest unit tests, which aides in testing equality in geometries with an optional threshold.

**geotrellis-raster**

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

**geotrellis-raster-testkit**

- Build test raster data.
- Assert raster data matches Array data or other rasters in scalatest.

**geotrellis-spark**

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

**geotrellis-spark-testkit**

- Utility code to create test RDDs of raster data.
- Matching methods to test equality of RDDs of raster data in scalatest unit tests.

**geotrellis-accumulo**

- Save and load layers to and from Accumulo. Query large layers efficiently using the layer query API.

**geotrellis-cassandra**

Save and load layers to and from Casandra. Query large layers efficiently using the layer query API.

**geotrellis-s3**

- Save/load raster layers to/from the local filesystem or HDFS using Spark's IO API.
- Save spatially keyed RDDs of byte arrays to z/x/y files in S3. Useful for saving PNGs off for use as map layers in web maps.

**geotrellis-etl**

- Parse command line options for input and output of ETL (Extract, Transform, and Load) applications
- Utility methods that make ETL applications easier for the user to build.
- Work with input rasters from the local file system, HDFS, or S3
- Reproject input rasters using a per-tile reproject or a seamless reprojection that takes into account neighboring tiles.
- Transform input rasters into layers based on a ZXY layout scheme
- Save layers into Accumulo, S3, HDFS or the local file system.

**geotrellis-shapefile**

- Read geometry and feature data from shapefiles into GeoTrellis types using GeoTools.

**geotrellis-slick**

- Save and load geometry and feature data to and from PostGIS using the slick scala database library.
- Perform PostGIS `ST_` operations in PostGIS through scala.
