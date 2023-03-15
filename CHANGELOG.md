<!-- markdownlint-disable MD024 -->
# Changelog

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Fix FileRangeReaderProvider parsing URI in windows [#3507](https://github.com/locationtech/geotrellis/pull/3507)

## [3.7.0] - 2023-02-26

### Added
- Add RasterSourceRDD.tiledLayerRDD within the geometry intersection [#3474](https://github.com/locationtech/geotrellis/pull/3474)
- Expose AWS_REQUEST_PAYER environment variable [#3479](https://github.com/locationtech/geotrellis/pull/3479)

### Changed
- Migration to CE3 and other major dependencies upgrade [#3389](https://github.com/locationtech/geotrellis/pull/3389)
- Revert graceful JTS fall back and lazy Circe encoders [#3463](https://github.com/locationtech/geotrellis/issues/3463)
- Update Cassandra up to 4.x [#3382](https://github.com/locationtech/geotrellis/issues/3382)
- Accumulo update up to 1.10.x [#3476](https://github.com/locationtech/geotrellis/pull/3476)
- Fixed Extent.translate [#3480](https://github.com/locationtech/geotrellis/pull/3480)
- liftCompletableFuture function fix [#3483](https://github.com/locationtech/geotrellis/pull/3483)
- Pass baseTiff to new RasterSource on GeoTiffResampleRasterSource.reproject/convert [#3485](https://github.com/locationtech/geotrellis/pull/3485)
- `Mask` and `InverseMask` operations preserve tile cell type [#3494](https://github.com/locationtech/geotrellis/pull/3494)

## [3.6.3] - 2022-07-12

### Changed
- Expose Charset in a ShapeFileReader API [#3464](https://github.com/locationtech/geotrellis/pull/3464)
- Bump Spark version up to 3.2.0 [#3471](https://github.com/locationtech/geotrellis/pull/3471)

## [3.6.2] - 2022-04-05

### Changed
- Dependencies update [#3452](https://github.com/locationtech/geotrellis/pull/3452)
- Lazy init Circe codecs in the vector module [#3457](https://github.com/locationtech/geotrellis/pull/3457)
- Lazy init Extent and ProjectedExtent Circe codecs [#3458](https://github.com/locationtech/geotrellis/pull/3458)
- Add a graceful JtsConfig fallback in case of a shapeless versions mismatch [#3459](https://github.com/locationtech/geotrellis/pull/3459)

## [3.6.1] - 2022-03-12

### Added
- Add new shading rules to make GT work with Spark 3.x [#3397](https://github.com/locationtech/geotrellis/pull/3397)
- Add Buffer Tile [#3419](https://github.com/locationtech/geotrellis/pull/3419)
- RasterSourceRDD.tiledLayerRDD should have a configurable partition transform function [#3450](https://github.com/locationtech/geotrellis/pull/3450)

### Changed
- Disambiguate withBufferTileFocalMethods implicit preserving bin compatibility [#3422](https://github.com/locationtech/geotrellis/pull/3422)
- Specialize Grid for Int and Long [#3428](https://github.com/locationtech/geotrellis/pull/3428)
- Move GeoWave and GeoMesa subproject to their own repositories [#3439](https://github.com/locationtech/geotrellis/pull/3439)
- Use JTS 1.18, GeoTools 25.0 [#3437](https://github.com/locationtech/geotrellis/pull/3437)
- ArrayTile.{cols | rows} calls boxing fix [#3441](https://github.com/locationtech/geotrellis/pull/3441)

## [3.6.0] - 2021-04-30

### Added
- Add method SpatialIndex#pointsInExtentAsIterable [#3349](https://github.com/locationtech/geotrellis/issues/3349)
- Spark 3 & Hadoop 3 Support [#3218](https://github.com/locationtech/geotrellis/issues/3218)
- Scala 2.13 cross compilation [#2893](https://github.com/locationtech/geotrellis/issues/2893)

### Changed
- Deprecate method SpatialIndex#traversePointsInExtent [#3349](https://github.com/locationtech/geotrellis/issues/3349)
- GDALRasterSource gives segmentation fault when reading rasters with NBITS=1 [#3300](https://github.com/locationtech/geotrellis/issues/3300)
- Make JTSConfig load lazy [#3369](https://github.com/locationtech/geotrellis/pull/3369)
- GDALWarpOptions incorrect rpc flag [#3370](https://github.com/locationtech/geotrellis/issues/3370)
- S3LayerDeleter cannot handle over 1000 objects to delete [#3371](https://github.com/locationtech/geotrellis/issues/3371)
- Drop Scala 2.11 cross compilation [#3259](https://github.com/locationtech/geotrellis/issues/3259)
- Fix MosaicRasterSource.tileToLayout behavior [#3338](https://github.com/locationtech/geotrellis/pull/3338)
- Replace geowave subproject with GeoTrellis/GeoWave data adapter [#3364](https://github.com/locationtech/geotrellis/pull/3364)

## [3.5.2] - 2021-02-01

### Added
- Add rasters S3 write methods [#3333](https://github.com/locationtech/geotrellis/pull/3333)
- Add initial GDAL Transform rotation support [#3331](https://github.com/locationtech/geotrellis/pull/3331)

### Changed
- Remove explicit unused Scaffeine dependency from projects [#3314](https://github.com/locationtech/geotrellis/pull/3314)
- Remove an excessive close after the abort call in S3RangeReader [#3324](https://github.com/locationtech/geotrellis/pull/3324)
- Fix sigmoidal contrast calculation [#3328](https://github.com/locationtech/geotrellis/pull/3328)
- Separated monoidal component of `vectortile.StrictLayer` [#3330](https://github.com/locationtech/geotrellis/pull/3330)

## [3.5.1] - 2020-11-23

### Changed
- Fix `Encoder[GeometryCollection]` including subclasses of GeometryCollection twice in the json
   (MultiPolygon, Multipoint,MultiLinestring) [#3167](https://github.com/locationtech/geotrellis/issues/3167)
- Fix `LayoutTileSource` buffer should only be 1/2 a cellsize to avoid going out of bounds and creating `NODATA` values [#3302](https://github.com/locationtech/geotrellis/pull/3302)
- Remove unused allocation from CroppedTile [#3297](https://github.com/locationtech/geotrellis/pull/3297)
- Fix GeometryCollection::getAll extension method [#3295](https://github.com/locationtech/geotrellis/pull/3295)
- Update gdal-warp-bindings v1.1.1 [#3303](https://github.com/locationtech/geotrellis/pull/3303)
  - gdal-warp-bindings 1.1.1 is a bugfix release that addresses a crash when initializing the bindings on MacOS. See:
    - https://github.com/geotrellis/gdal-warp-bindings#macos
    - https://github.com/geotrellis/gdal-warp-bindings/pull/99

## [3.5.0] - 2020-08-18

### Added
- Add alternative RasterizeRDD methods for keyed geometries or features [#3271](https://github.com/locationtech/geotrellis/pull/3271)

### Changed
- Fix NoData handling in the ColorMethods.color function for the RGB and RGBA Multiband Rasters [#3278](https://github.com/locationtech/geotrellis/pull/3278)
- Fix renderJpg() color is bluer than expected [#3203](https://github.com/locationtech/geotrellis/issues/3203)
- Fix combineDouble of ArrayTile and ConstantTile with non-commutative operator [#3257](https://github.com/locationtech/geotrellis/issues/3257)
- Update GDAL up to 3.1 [#3279](https://github.com/locationtech/geotrellis/pull/3279)
- Fix GeoTiff writer does not currently support WebMercator projection with no EPSG code set [#3281](https://github.com/locationtech/geotrellis/issues/3281)
- Fix Tile combine should union cellTypes [#3284](https://github.com/locationtech/geotrellis/pull/3284)
- Fix CRS.fromWKT function throws [#3209](https://github.com/locationtech/geotrellis/issues/3209)

## [3.4.1] - 2020-07-16

### Changed
- GeoTiffRasterSource is not threadsafe enough [#3265](https://github.com/locationtech/geotrellis/pull/3265)
- Fix ShortArrayTileResult result ArrayTile fulfillment [#3268](https://github.com/locationtech/geotrellis/pull/3268)
- GeoTiffRasterSource TiledToLayout reads by SpatialKey can produce non-256x256 tiles [3267](https://github.com/locationtech/geotrellis/issues/3267)
- Fix Overviews Read Incorrectly when Per Dataset Masks Present [#3269](https://github.com/locationtech/geotrellis/issues/3269)

## [3.4.0] - 2020-06-19

### Added
- Add GS in the list of supported URI schemes [#3235](https://github.com/locationtech/geotrellis/pull/3235)
- WKT Proj4 Extension support [#3241](https://github.com/locationtech/geotrellis/issues/3241)

### Changed
- Fix GeoTrellisRasterSources to properly pass time though all the internal functions [#3226](https://github.com/locationtech/geotrellis/pull/3226)
- Move GDAL overview strategy logger to debug level [#3230](https://github.com/locationtech/geotrellis/pull/3230)
- Fix S3RDDLayerReader partitioning [#3231](https://github.com/locationtech/geotrellis/pull/3231)
- GDALRasterSource works inconsistenly with BitCellType and ByteCellType [#3232](https://github.com/locationtech/geotrellis/issues/3232)
- rasterizeWithValue accepts only topologically valid polygons [#3236](https://github.com/locationtech/geotrellis/pull/3236)
- Rasterizer.rasterize should be consistent with rasterizeWithValue [#3238](https://github.com/locationtech/geotrellis/pull/3238)
- GeoTrellisRasterSource should return None on empty reads [#3240](https://github.com/locationtech/geotrellis/pull/3240)
- ArrayTile equals method always returns true if first elements are NaN [#3242](https://github.com/locationtech/geotrellis/issues/3242)
- Fixed resource issue with JpegDecompressor that was causing a "too many open files in the system" exception on many parallel reads of JPEG compressed GeoTiffs. [#3249](https://github.com/locationtech/geotrellis/pull/3249)
- Fix MosaicRasterSource, GDALRasterSource and GeoTiffResampleRasterSource behavior [#3252](https://github.com/locationtech/geotrellis/pull/3252)
- HttpRangeReader should live outside of the Spark package [#3254](https://github.com/locationtech/geotrellis/issues/3254)
- HttpRangeReader moved to `geotrellis.util [#3256](https://github.com/locationtech/geotrellis/issues/3256)
- Consistently construct GridExtents with `math.round` [#3248](https://github.com/locationtech/geotrellis/issues/3248)

## [3.3.0] - 2020-04-07

### Added

- GeoTrellisPath assumes `file` scheme when none provided [#3191](https://github.com/locationtech/geotrellis/pull/3191)
- toStrings overrides to common classes [#3217](https://github.com/locationtech/geotrellis/pull/3217)
- GeoTrellisRasterSources legacy and temporal layers support [#3179](https://github.com/locationtech/geotrellis/issues/3179)

### Changed

- Fix `OverviewStrategy` instances now define their own overview selection logic and more accurately port GDAL-Warp -ovr options [#3196](https://github.com/locationtech/geotrellis/issues/3196)
- Fix `PolygonRasterizer` failure on some inputs [#3160](https://github.com/locationtech/geotrellis/issues/3160)
- Fix GeoTiff Byte and UByte CellType conversions [#3189](https://github.com/locationtech/geotrellis/issues/3189)
- Fix incorrect parsing of authority in GeoTrellisPath [#3191](https://github.com/locationtech/geotrellis/pull/3191)
- GeoTrellisPath.zoomLevel is now `Option[Int]` -> `Int` to better indicate that it is a required parameter [#3191](https://github.com/locationtech/geotrellis/pull/3191)
- Fix the length of one degree at the equator in metres [#3197](https://github.com/locationtech/geotrellis/pull/3197)
- Fix Fix DelaunayRasterizer [#3202](https://github.com/locationtech/geotrellis/pull/3202)
- RasterSources resolutions should be consistent across implementations [#3210](https://github.com/locationtech/geotrellis/issues/3210)
- Bump gdal-warp-bindings version up to 1.0.0 [#3211](https://github.com/locationtech/geotrellis/pull/3211)
- Fixed GDALRasterSource.resample method behavior to respect the passed resampleMethod [#3211](https://github.com/locationtech/geotrellis/pull/3211)
- Fix Jackson dependencies [#3212](https://github.com/locationtech/geotrellis/issues/3212)
- Fix Rasterizer for polygons containing concavities whith `PixelIsArea` option [#3192](https://github.com/locationtech/geotrellis/pull/3192)
- Fix spatial join (Spark) when using different partitioning in left and right RDDs [#3175](https://github.com/locationtech/geotrellis/pull/3175)
- Fix Monad instance for `PolygonalSummaryResult` [#3221](https://github.com/locationtech/geotrellis/pull/3221)

### Removed

- Removed duplicate JSON codecs for `TileLayout` [#3181](https://github.com/locationtech/geotrellis/pull/3181)
- Removed `private` restriction for TiffTag `read` overload [#3181](https://github.com/locationtech/geotrellis/pull/3181)

## [3.2.0] - 2019-11-19

### Changed

- Preserve NODATA values for double cell types when resampling with Max or Min resampler [#3144](https://github.com/locationtech/geotrellis/pull/3144)
- Update dependency versions for Scala 2.12 cross build [#3132](https://github.com/locationtech/geotrellis/pull/3132)
- Fix eager evaluation of band min-max in `GDALDataset` [#](https://github.com/locationtech/geotrellis/pull/3162)

## [3.1.0] - 2019-10-25

### Changed

- Fix `StreamingByteReader` over-allocation when reading past EOF [#3138](https://github.com/locationtech/geotrellis/pull/3138)
- `Tile`, `ArrayTile`, `ConstantTile`, `DelegatingTile`, `MultibandTile`, `MultibandArrayTile`, `RasterRegion`, `RasterSource`, `MosaicRasterSource` converted from `trait` to `abstract class` [#3136](https://github.com/locationtech/geotrellis/pull/3136)

## [3.0.0] - 2019-10-18

### Added

- RasterSources API [#3053](https://github.com/locationtech/geotrellis/pull/3053)
- `geotrellis.layer` package to group functionality related to tiled layers.
  - `SpatialKey`, `SpaceTimeKey`, `TemporalKey`, `Bounds`, `Boundable`, `EmptyBounds`, `KeyBounds`, `TemporalProjectedExtent`, `EmptyBoundsError`, `SpatialComponent`, `TemporalComponent`, `LayoutDefinition`, `LayoutScheme`, `LayoutLevel`, `LocalLayoutScheme`, `FloatingLayoutScheme`, `ZoomedLayoutScheme`, `MapKeyTransform`, etc.
- `geotrellis.store` package to group interfaces related to saving and reading tiled layers. Implementations save moved to:
  - `geotrellis.store.accumulo`
  - `geotrellis.store.cassandra`
  - `geotrellis.store.hbase`
  - `geotrellis.store.hadoop`
  - `geotrellis.store.s3`
- `geotrellis.vectortile.MVTFeature` which properly conforms to the MVT 2.0 spec. Specifically, `MVTFeature` adds `id: Option[Long]` property.
- `RangeReader` SPI produced an instance of the reader from `URI` [#2998](https://github.com/locationtech/geotrellis/pull/2998)
- `geotrellis.raster.mapalgebra.focal.ZFactor` to improve slope calculations [#3014](https://github.com/locationtech/geotrellis/pull/3014)
- A `sparseStitch` method on `geotrellis.layer.stitch.SpatialTileLayoutCollectionStitchMethods`. Note that `SpatialTileLayoutCollectionStitchMethods` now has the additional constraint `geotrellis.raster.prototype.TilePrototypeMethods` on type `V`. This should be transparent for users of the `geotrellis.raster.Tile` and `geotrellis.raster.MultibandTile` types [#3017](https://github.com/locationtech/geotrellis/pull/3017)
- `geotrellis.util.np` package which contains `linspace` and `percentile`methods that match NumPy functionality. An implicit class was also added to `geotrellis.raster` which provides the `percentile` method for `geotrellis.raster.Tile` [#3067](https://github.com/locationtech/geotrellis/pull/3067)
- Conversion from raster to features of cells [#3117](https://github.com/locationtech/geotrellis/pull/3117)
- `io.circe` JSON encoders and decoders for `GeoTiffInfo` and related classes [#3128](https://github.com/locationtech/geotrellis/pull/3128)

### Changed

- `geotrellis.raster.summary.polygonal.[Multi]TilePolygonalSummaryHandler` replaced with `geotrellis.raster.summary.polygonal.PolygonalSummary`. Users should expect to implement concrete subclasses of `geotrellis.raster.summary.GridVisitor` and pass those to the new polygonalSummary methods. There are a number of default implementations provided for simple operations in `geotrellis.raster.summary.visitors`
- Polygonal summaries on raster RDDs of `RDD[(SpatialKey, T <: Grid[Int])] with Metadata[TileLayerMetadata[SpatialKey]]` can now be performed with far less boilerplate using the same visitor pattern as the new raster polygonal summary API. See `RDDPolygonalSummary.scala` for additional details.
- `geotrellis.raster.Grid.dimensions` is now `Dimensions[N]` instead of `(N, N)` [#3124](https://github.com/locationtech/geotrellis/pull/3124)
- `GeoTiffInfo` moved out of `GeoTiffReader` into containing package [#3128](https://github.com/locationtech/geotrellis/pull/3128)
- Replaced use of internal `Mergable` trait with cats' `Semigroup`
- The `slope` focal method now requires a `ZFactor` instance.
- Use the AWS S3 SDK v2 instead of v1 [#2911](https://github.com/locationtech/geotrellis/pull/2911).
- The implicit/package structure of the `geotrellis.raster` package has changed such that it's now possible to import almost all features/extensions with `import geotrellis.raster._` [#2891](https://github.com/locationtech/geotrellis/pull/2891)
- `geotrellis.pro4j.WKT` object conversion methods to and from EPSG codes have changed. All now return Options rather than silently unwrap. Methods that convert from EPSG code to WKT string are now prefixed consistently with `fromEpsg` and methods that convert from WKT string to EPSG code are now prefixed consistently with `toEpsg`.
- `geotrellis.util.CRS.fromWKT` now returns `Option[CRS]` instead of `CRS` after silently unwrapping an optional internally
  - The `geotrellis.vectortile.{Layer, VectorTile}` interfaces now uses `MVTFeature` instead of `geotrellis.vector.Feature`.
- Change from `scala-logging` to `log4s` [#3116](https://github.com/locationtech/geotrellis/pull/3116)
- Update dependencies [#2904](https://github.com/locationtech/geotrellis/pull/2904).
- Update dependencies [#3053](https://github.com/locationtech/geotrellis/pull/3053).
- Bump ScalaPB version up with some API enhancements [#2898](https://github.com/locationtech/geotrellis/pull/2898).
- Artifacts in Viewshed have been addressed, the pixels/meter calculation has also been improved [#2917](https://github.com/locationtech/geotrellis/pull/2917).
- Fix map{X|Y}ToGrid function behavior that could give a bit incorrect results [#2953](https://github.com/locationtech/geotrellis/pull/2953).
- Fix COG layer update bug related to `COGLayerMetadata` zoom ranges ordering [#2922](https://github.com/locationtech/geotrellis/pull/2922).
- Use original `ZoomRanges` on COG layer update [#2956](https://github.com/locationtech/geotrellis/pull/2956).
- `ArrayTile` equality will now check the cols, rows, and cellType of the two tiles [#2991](https://github.com/locationtech/geotrellis/pull/2991).
- Fix incorrect Deflate compressor usage in GeoTiff reading [#2997](https://github.com/locationtech/geotrellis/pull/2997).
- Refactor IO thread pool usage [#3007](https://github.com/locationtech/geotrellis/pull/3007).
- `S3RangeReader` will now optionally read the HEADER of an object [#3025](https://github.com/locationtech/geotrellis/pull/3025).
- `FileRangeReaderProvider` can now handle more types of `URI`s [#3034](https://github.com/locationtech/geotrellis/pull/3034).
- Bump `proj4` version to fix multiple performance issues [#3039](https://github.com/locationtech/geotrellis/pull/3039).
- Fix `HttpRangeReader` swallows 404 error [#3073](https://github.com/locationtech/geotrellis/pull/3073)
- Add a ToSpatial function for the collections API [#3082](https://github.com/locationtech/geotrellis/pull/3082).
- Fix `TIFFTagsReader` to skip unsupported tags [#3088](https://github.com/locationtech/geotrellis/pull/3088).
- `reprojectExtentAsPolygon` should be more deterministic [#3083](https://github.com/locationtech/geotrellis/pull/3083).
- `AmazonS3URI.getKey` now returns an empty string instead of null if no key was provided [#3096](https://github.com/locationtech/geotrellis/pull/3096).
- Improved handling of null prefix for `S3AttributeStore`. Prefer use of `S3AttributeStore.apply` to take advantage of this improved handling [#3096](https://github.com/locationtech/geotrellis/pull/3096).
- Fix Tiled TIFF BitCellType Segments conversion [#3102](https://github.com/locationtech/geotrellis/pull/3102)
- Updated `computeResolution` in `IterativeCostDistance` to match Iterative Viewshed [#3106](https://github.com/locationtech/geotrellis/pull/3106)

### Removed

- Scala wrappers of JTS Geometry in `geotrellis.vector` [#2932](https://github.com/locationtech/geotrellis/pull/2932)
- `geotrellis.etl` package has been removed. The code has been archived at <https://github.com/geotrellis/spark-etl> [#2969](https://github.com/locationtech/geotrellis/pull/2969).
- The `S3Client` wrapper that was used to support AWS SDK v1 has been removed in favor of directly using AWS SDK S3Client [#2911](https://github.com/locationtech/geotrellis/pull/2911).
- `geotrellis.s3-testkit` has been removed; testing now depends on an external min.io server which mocks the S3 API. [#2911](https://github.com/locationtech/geotrellis/pull/2911).
- `geotrellis.slick` has been removed. Slick support will now reside in `geotrellis-contrib` [#2902](https://github.com/locationtech/geotrellis/pull/2902).

## [2.3.0] - 2019-04-18

### Changed

- Fix Accumulo and HBase `AttributeStore` performance ([#2899](https://github.com/locationtech/geotrellis/pull/2899)).
  - Fix Cassandra AttributeStore performance ([#2901](https://github.com/locationtech/geotrellis/pull/2901)).
  - Fix `createAlignedGridExtent` function actually to align to GridExtents ([#2878](https://github.com/locationtech/geotrellis/pull/2878)).
  - Fix to `rasterRegionReproject` function ([#2880](https://github.com/locationtech/geotrellis/pull/2880)).

## [2.2.0] - 2019-01-11

### Changed

- Extract Proj4J to `org.locationtech.proj4j` ([#2846](https://github.com/locationtech/geotrellis/pull/2846)).
- Fix {Backend}LayerCopy exception handling ([#2860](https://github.com/locationtech/geotrellis/pull/2860)).
- Fix cache level in Ingest objects ([#2854](https://github.com/locationtech/geotrellis/pull/2854)).
- Catch a possible precision loss on reprojection to the same projection ([#2849](https://github.com/locationtech/geotrellis/pull/2849)).
- Make histogram.merge methods more specific ([#2852](https://github.com/locationtech/geotrellis/pull/2852)).
- Fixed elliptical radius check in IDW algorithm ([#2844](https://github.com/locationtech/geotrellis/pull/2844)).
- Replace all JavaConversions imports by JavaConverters ([#2843](https://github.com/locationtech/geotrellis/pull/2843)).
- Made implicit conversions to/from Raster and ProjectedRaster deprecated ([#2834](https://github.com/locationtech/geotrellis/pull/2834)).
- Fix vectorizer bug ([#2839](https://github.com/locationtech/geotrellis/pull/2839)).
- Added `mapTile` to `ProjectedRaster` ([#2830](https://github.com/locationtech/geotrellis/pull/2830)).
- Downgrade Avro version ([#2837](https://github.com/locationtech/geotrellis/pull/2837)).
- Fix error in `ReprojectRasterExtent` ([#2825](https://github.com/locationtech/geotrellis/pull/2825)).
- Dependency updates ([#2813](https://github.com/locationtech/geotrellis/pull/2813)).
- Bump Spark version up to 2.4 ([#2823](https://github.com/locationtech/geotrellis/pull/2823)).
- Use a local[2] mode inside TestEnvironment trait ([#2821](https://github.com/locationtech/geotrellis/pull/2821)).
- Make IO code agnostic about effects ([#2818](https://github.com/locationtech/geotrellis/pull/2818)).
- Make IO code agnostic about effects ([#2818](https://github.com/locationtech/geotrellis/pull/2818)).

## [2.1.0] - 2018-10-02

### Added

- `geotrellis.spark.etl.TemporalIngest` main method ([#2709](https://github.com/locationtech/geotrellis/pull/2709)).
- Add `bbox` field to all GeoJSON Features ([#2811](https://github.com/locationtech/geotrellis/pull/2811)).

### Changed

- `TileRDDReproject` now works on RDD of `TileFeature[T, D]` ([#2803](https://github.com/locationtech/geotrellis/pull/2803)).
- `TileRDDReproject` now uses `Reproject.Options.errorThreshold` value ([#2803](https://github.com/locationtech/geotrellis/pull/2803)).
- `geotrellis.spark.testkit.TestEnvironment` now includes `SparkSession` ([#2808](https://github.com/locationtech/geotrellis/pull/2808)).
- `geotrellis.raster.RasterRegionReproject` defaults to approximate resampling for `MultibandBandTile` ([#2803](https://github.com/locationtech/geotrellis/pull/2803)).
- `Stitcher` type class instance is now defined for `TileFeature[T, D]` ([#2803](https://github.com/locationtech/geotrellis/pull/2803)).
- Fix `GeoTiffSegment` conversion to `UByteCellType` and `UShortCellType` ([#2794](https://github.com/locationtech/geotrellis/pull/2794)).
- Fix `GeoTiff[T]` interpretation of `AutoHigherResolution` `OverviewStrategy` ([#2802](https://github.com/locationtech/geotrellis/pull/2802)).
- `GeoTiff[T].getClosestOverview` method is now public ([#2804](https://github.com/locationtech/geotrellis/pull/2804)).
- `GeoTiffOptions.storageMethod` now defaults to `Tiled` instead of `Striped` ([#2810](https://github.com/locationtech/geotrellis/pull/2810)).
- GeoTiff `TiffTags` class can now read inverted extents ([#2812](https://github.com/locationtech/geotrellis/pull/2812)).

## [2.0.0] 2019-01-11

### Added

- All focal operations now except an optional `partitioner` parameter.
- `BufferTiles.apply` methods and the `bufferTiles` methods now except an optional `partitioner` parameter.
- `CollectionLayerReader` now has an SPI interface.
- `ZoomResample` can now be used on `MultibandTileLayerRDD`.
- `Partitioner` can be specified in the `reproject` methods of `TileLayerRDD`.
- Compression `level` of GeoTiffs can be specified in the `DeflateCompression` constructor.
- `resampleMethod` parameter has been added to `COGLayerWriter.Options`.
- A new type called `LayerType` has been created to help identify the nature of a layer (either Avro or COG).
- `LayerHeader` now have an additional parameter: `layerType`.
- `AttributeStore` now has four new methods: `layerType`, `isCOGLayer`, `readCOGLayerAttributes`, and `writeCOGLayerAttributes`.
- Kryo serialization of geometry now uses a binary format to reduce shuffle block size.
- Alter `geotrellis.spark.stitch.StitchRDDMethods` to allow `RDD[(K, V)]` to be stitched when not all tiles are of the same dimension.
- Introduce `Pyramid` class to provide a convenience wrapper for building raster pyramids.
- Both `COGValueReader` and `OverzoomingCOGValueReader` now have the `readSubsetBands` method which allows users to read in a select number of bands in any order they choose.
- `COGLayerReader` now has the `readSubsetBands` and `querySubsetBands` methods which allow users to read in layers with the desired bands in the order they choose.
- `KeyBounds` now has the `rekey` method that will rekey the bounds from a source layout to a target layout.
- Kryo serialization of geometry now uses a binary format to reduce shuffle block size.
- `GeoTiffMultibandTile` now has another `crop` method that takes a `GridBounds` and an `Array[Int]` that represents the band indices.
- `MultibandTile` now has a new method, `cropBands` that takes an Array of band indices and returns a cropped `MultibandTile` with the chosen bands.
- `GeoTiff[MultibandTile]` can be written with `BandInterleave`, only `PixelInterleave` previously supported. ([#2767](https://github.com/locationtech/geotrellis/pull/2767))
- `Input.partitionBytes` and it is set to `134217728` by default to correspond `HadoopGeoTiffRDD default behaviour`.
- `Output.bufferSize` option to set up a custom buffer size for the buffered reprojection.

### Changed

- The length of the key (the space-filling curve index or address) used for layer reading and writing has been extended from a fixed length of 8 bytes to an arbitrary length. This change affects not only the `geotrellis.spark` package, but all backends (excluding `geotrellis.geowave` and `geotrellis.geomesa`).
- Reprojection has improved performance due to one less shuffle stage and lower memory usage. `TileRDDReproject` loses dependency on `TileReprojectMethods` in favor of `RasterRegionReproject`
- The Ascii draw methods are now method extensions of `Tile`.
- Replace `geotrellis.util.Functor` with `cats.Functor`.
- Specifying the `maxTileSize` for a COGLayer that's to be written is now done via `COGLayerWriter.Options` which can be passed directly to the `write` methods.
- Specifying the `compression` for a COGLayer that's to be written is now done via `COGLayerWriter.Options` which can be passed directly to the `write` methods.
- The attribute name for `COGLayerStorageMetadata` is now `metadata` instead of `cog_metadata`.
- `Scalaz` streams were replaced by `fs2` streams.
- Refactored `HBaseInstance`, now accepts a plain Hadoop `Configuration` object.
- Refactored `CassandraInstance`, now accepts a `getCluster` function.
- Use `pureconfig` to handle all work with configuration files.
- Change `TilerMethods.tileToLayout` functions that accept `TileLayerMetadata` as an argument to return `RDD[(K, V)] with Metadata[M]` instead of `RDD[(K, V)]`.
- Expose `attributeStore` parameter to LayerReader interface.
- Added exponential backoffs in `S3RDDReader`.
- Changed `SinglebandGeoTiff` and `MultibandGeoTiff` crop function behavior to work properly with cases when extent to crop by doesn't intersect tiff extent.
- All classes and objects in the `geowave` package now use the spelling: `GeoWave` in their names.
- `TileLayerMetadata.fromRdd` method has been renamed to `TileLayerMetadata.fromRDD`.
- `KeyBounds.fromRdd` method has been renamed to `KeyBounds.fromRDD`.
- Removed implicit conversion from `Raster[T]` to `T` ([#2771](https://github.com/locationtech/geotrellis/pull/2771)).
- Removed `decompress` option from `GeoTiffReader` functions.
- `geotrellis.spark-etl` package is deprecated since GeoTrellis 2.0.
- `Input.maxTileSize` is `256` by default to correspond `HadoopGeoTiffRDD` default behavior.
- `geotrellis.slick.Projected` has been moved to `geotrellis.vector.Projected`
- StreamingHistogram.binCount now returns non-zero counts ([#2590](https://github.com/locationtech/geotrellis/pull/2590))
- HilbertSpatialKeyIndex index offset. Existing spatial layers using Hilbert index will need to be updated ([#2586](https://github.com/locationtech/geotrellis/pull/2586))
- Fixed `CastException` that sometimes occurred when reading cached attributes.
- Uncompressed `GeoTiffMultibandTiles` will now convert to the correct `CellType`.
- Calculating the Slope of a `Tile` when `targetCell` is `Data` will now produce the correct result.
- Introduce new hooks into `AttributeStore` trait to allow for better performance in certain queries against catalogs with many layers.
- `GeoTiffReader` can now read tiffs that are missing the `NewSubfileType` tag.
- Pyramiding code will once again respect resampling method and will now actually reduce shuffle volume by resampling tiles on map side of pyramid operation.
- `COGLayer` attributes can be accessed via the various read attribute methods in `AttributeStore` (ie `readMetadata`, `readHeader`, etc)
- The regex used to match files for the `HadoopLayerAttributeStore` and `FileLayerAttributeStore` has been expanded to include more characters.
- `HadoopAttributeStore.availableAttributes` has been fixed so that it'll now list all attribute files.
- Allow for simple features to be generated with a specified or random id with geometry stored in the standard field, `the_geom`
- Update version of Amazon SDK API to remove deprecation warnings.
- Fixed a bug in incorrect metadata fetch by `COGLayerReader` that could lead to an incorrect data querying.
- Cropping RDDs with `clamp=false` now produces correct result.
- Fixed tiff reads in case `RowsPerStrip` tiff tag is not defined.
- Change aspect result to azimuth, i.e. start from due north and be clockwise.
- COG overviews generated in the `COGLayer.fromLayerRDD` method will now use the passed in `ResampleMethod`.
- Reading a GeoTiff with `streaming` will now work with files that are larger than `java.lang.Integer.MAX_VALUE`.
- `GeoTiffMultibandTile.crop` will now work with GeoTiffs that have tiled segments and band interleave.
- `GeoTiffMultibandTile.crop` will now return `ArrayMultibandTile`(s) with the correct number of bands.
- Improved performance of `COGValueReader.readSubsetBands` when reading from S3.

### Removed

- `LayerUpdater` with its functionality covered by `LayerWriter` ([#2663](https://github.com/locationtech/geotrellis/pull/2663)).

## [1.2.1] - 2018-01-03

### Changed

- [GeoTiffSegmentLayout.getIntersectingSegments bounds checking](https://github.com/locationtech/geotrellis/pull/2534)
- [Fix for area of vectorizer that can throw topology exceptions](https://github.com/locationtech/geotrellis/pull/2530)
- [Fix Tile.flipHorizontal for floating point tiles](https://github.com/locationtech/geotrellis/pull/2535)

## [1.2.0] - 2017-11-06

This release cycle saw a regular contributor [Simeon Fitch](https://github.com/metasim) elevated to official *Committer* status within GeoTrellis.

The team would like to thank him, along with our newest contributors [Aaron Santos](https://github.com/aaron-santos), [Austin Heyne](https://github.com/aheyne), [Christos Charmatzis](https://github.com/Charmatzis), [Jessica Austin](https://github.com/jessicaaustin), and [@mteldridge](https://github.com/mteldridge) for helping make this release possible.

### Changed

- `geotrellis.raster`
  - **Deprecation:** `GridBounds.size` in favor of `GridBounds.sizeLong`.
  - **Deprecation:** `GridBounds.coords` in favor of `GridBounds.coordsIter`.
  - **New:** `GridBounds.offset` and `GridBounds.buffer` for creating a modified `GridBounds` from an existing one.
  - **New:** `ColorRamps.greyscale: Int => ColorRamp`, which will generate a ramp when given some number of stops.
  - **New:** `ConstantTile.fromBytes` to create any type of `ConstantTile` from an `Array[Byte]`.
  - **New:** `Tile.rotate90: Int => Tile`, `Tile.flipVertical: Tile` and `Tile.flipHorizontal: Tile`.
- `geotrellis.vector`
  - **New:** `Geometry.isEmpty: Boolean`. This incurs much less overhead than previous ways of determining emptiness.
  - **New:** `Line.head` and `Line.last` for **efficiently** grabbing the first or last `Point` in the `Line`.
- `geotrellis.spark`
  - **Deprecation:** The `LayerUpdater` trait hierarchy. Use `LayerWriter.update` or `LayerWriter.overwrite` instead.
  - **Deprecation:** Every cache provided by `geotrellis.spark.util.cache`. These will be removed in favor of a pluggable cache in 2.0.
  - **New:** `SpatialKey.extent: LayoutDefinition => Extent`
  - **New:** `ValueReader.attributeStore: AttributeStore`
  - **New:** `TileLayerRDD.toSpatialReduce: ((V, V) => V) => TileLayerRDD[SpatialKey]` for smarter folding of 3D tile layers into 2D tile layers.
  - The often-used `apply` method overloads in `MapKeyTransform` have been given more descriptive aliases.
  - **Change:** Querying a layer will now produce a result whose metadata will have an `Extent` and `KeyBounds` of the queried region and not of the whole layer.
- `geotrellis.vectortile` (experimental)
  - **New:** `VectorTile.toGeoJson` and `VectorTile.toIterable`.
  - Library simplified by assuming the codec backend will always be protobuf.

### Added

#### Rasterizing `Geometry` Layers

Finally, the full marriage of the `vector`, `raster`, and `spark`
packages! You can now transform an `RDD[Geometry]` into a writable
GeoTrellis layer of `(SpatialKey, Tile)`!

```scala
val geoms: RDD[Geometry] = ...
val celltype: CellType = ...
val layout: LayoutDefinition = ...
val value: Double = ...  /* Value to fill the intersecting pixels with */

val layer: RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] =
  geoms.rasterize(value, celltype, layout)
```

#### Clipping `Geometry` Layers to a Grid

In a similar vein to the above, you can now transform an arbitrarily
large collection of Geometries into a proper GeoTrellis layer, where the
sections of each Geometry are clipped to fit inside their enclosing
Extents.

![Clipping Geometry to Grid](img/cliptogrid.png)

Here we can see a large `Line` being clipped into nine sublines. It's
one method call:

```scala
import geotrellis.spark._

val layout: LayoutDefinition = ...  /* The definition of your grid */
val geoms: RDD[Geometry] = ...      /* Result of some previous work */

/* There are likely many clipped Geometries per SpatialKey... */
val layer: RDD[(SpatialKey, Geometry)] = geoms.clipToGrid(layout)

/* ... so we can group them! */
val grouped: RDD[(SpatialKey, Iterable[Geometry])] = layer.groupByKey
```

If clipping on the Extent boundaries is not what you want, there are ways to customize this. See [the ClipToGrid entry in our Scaladocs](https://geotrellis.github.io/scaladocs/latest/#geotrellis.spark.clip.ClipToGrid$).

#### Sparkified Viewshed

A [Viewshed](https://en.wikipedia.org/wiki/Viewshed) shows "visibility" from some set vantage point, given an Elevation raster. Prior to GeoTrellis 1.2 this was possible at the individual `Tile` level but not the Layer (`RDD`) level. Now it is.

First, we need to think about the `Viewpoint` type:

```scala
import geotrellis.spark.viewshed._

val point: Viewpoint(
  x = ...,                    // some coordinate.
  y = ...,                    // some coordinate.
  viewHeight = 4000,          // 4 kilometres above the surface.
  angle = Math.PI / 2,        // direction that the "camera" faces (in radians). 0 == east.
  fieldOfView = Math.PI / 2,  // angular width of the "view port".
  altitude = 0                // the height of points you're interested in seeing.
)
```

In other words:

- x, y, viewHeight: where are we?
  - angle: what direction are we looking?
  - fieldOfView: how wide are we looking?
  - altitude: how high/low is the "target" of our viewing?

Given a `Seq[Viewpoint]` (the algorithm supports multiple simultaneous
view points), we can do:

```scala
// Recall this common alias:
//   type TileLayerRDD[K] = RDD[(K, Tile)] with Metadata[TileLayerMetadata[K]]

val layer: TileLayerRDD[SpatialKey] = ...  /* Result of previous work */

val viewshed: TileLayerRDD[SpatialKey] = layer.viewshed(Seq(point))
```

#### Sparkified Euclidean Distance

We use *Euclidean Distance* to render a collection of points into a
heat-map of proximities of some area. Say, of two roads crossing:

![Euclidean Distance Diagram](img/euclid.png)

Prior to GeoTrellis 1.2, this was possible at the individual `Tile`
level but not the Layer (`RDD`) level. Now it is.

```scala
/* Result of previous work. Potentially millions of points per SpatialKey. */
val points: RDD[(SpatialKey, Array[Coordinate])] = ...
val layout: LayoutDefinition = ...  /* The definition of your grid */

val layer: RDD[(SpatialKey, Tile)] = points.euclideanDistance(layout)
```

#### Polygonal Summaries over Time

The following was possible prior to GeoTrellis 1.2:

```scala
val layer: TileLayerRDD[SpatialKey] = ...
val polygon: Polgyon = ...

/* The maximum value within some Polygon overlaid on a Tile layer */
val summary: Double = layer.polygonalMaxDouble(polygon)
```

The above is also now possible for layers keyed by `SpaceTimeKey` to
form a "time series":

```scala
val layer: TileLayerRDD[SpaceTimeKey] = ...
val polygon: MultiPolygon = ...

/* The maximum value within some Polygonal area at each time slice */
val summary: Map[ZonedDateTime, Double] = layer.maxSeries(polygon)
```

#### `OverzoomingValueReader`

A GeoTrellis `ValueReader` connects to some layer catalog and lets you
read individual values (usually Tiles):

```scala
import geotrellis.spark.io.s3._

val store: AttributeStore = ...
val reader: Reader[SpatialKey, Tile] = S3ValueReader(store).reader(LayerId("my-catalog", 10))

val tile: Tile = reader.read(SpatialKey(10, 10))
```

However `.reader` is limited to zoom levels that actually exist for the
given layer. Now you can use `.overzoomingReader` to go as deep as you
like:

```scala
import geotrellis.raster.resample._

val reader: Reader[SpatialKey, Tile] =
  S3ValueReader(store).overzoomingReader(LayerId("my-catalog", 20), Average)

val tile: Tile = reader.read(SpatialKey(1000, 1000))
```

#### Regridding a Tile Layer

Have you ever wanted to "redraw" a grid over an established GeoTrellis
layer? Say, this 16-tile Layer into a 4-tile one, both of 1024x1024
total pixels:

![Regrid Diagram](img/regrid.png)

Prior to GeoTrellis 1.2, there was no official way to do this. Now you
can use `.regrid`:

```scala
/* The result of some previous work. Say each Tile is 256x256. */
val layer: TileLayerRDD[SpatialKey] = ...

/* "Recut" the tiles so that each one is now 512x512.
 * No pixels are gained or lost, save some NODATA on the bottom
 * and right edges that may appear for padding purposes.
 */
val regridded: TileLayerRDD[SpatialKey] = layer.regrid(512)
```

You can also regrid to non-rectangular sizes:

```scala
val regridded: TileLayerRDD[SpatialKey] = layer.regrid(tileCols = 100, tileRows = 300)
```

#### Robust Layer Querying

It's common to find a subset of Tiles in a layer that are touched by
some given `Polygon`:

```scala
val poly: Polygon = ???

val rdd: TileLayerRDD[SpatialKey] =
 layerReader
    .query[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](Layer("name", zoom))
    .where(Intersects(poly))
    .result
```

Now you can perform this same operation with `Line`, `MultiLine`, and
even `(Polygon, CRS)` to ensure that your Layer and Geometry always
exist in the same projection.

#### Improved `Tile` ASCII Art

Sometimes you just want to visualize a `Tile` without going through the
song-and-dance of rendering it to a `.png`. The existing
`Tile.asciiDraw` method *kind of* does that, except its output is all in
numbers.

The new `Tile.renderAscii: Palette => String` method fulfills your heart's desire:

```scala
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.render.ascii._

val tile: Tile = SinglebandGeoTiff("path/to/tiff.tiff").tile

// println(tile.renderAscii())  // the default
println(tile.renderAscii(AsciiArtEncoder.Palette.STIPLED))
```

```console
            ▚▖
            ▚▚▜▚▚
            ▚▖▚▜▚▖▚▚
           ▜▚▚▚▜▚█▚▜▚█▚
           █▚▜▖▜▖▚▚█▚▚▜▚█▖
           ▚▚█▚▜▚▚▚▚▚▚▚▜▚▚▚▚▚
          ▚▚▖▚▚▚▚▚█▜▚▚▜▚▚▖▚▖▚▖▚
          ▚▚▚▚█▚▚▚▚▚██▚▚▚▜▖▖██▚▚▜▚
          ▚▚█▚▚▚▚▚▚▚▜▚▚▚▚▚▚▜▚█▚▚▚▚▚▚▚
         █▚▚▖▚█▚▜▚▚▚▚▖▚▚▚▚▚▚▚▚▚▚▜▚▚▚▚▚▚▖
         █▚▚▚▜▚▖▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚▚██▖▜▚█▚▚▚
         █▚▚██▚▚▚▚▚▚▚▚▖▚▚▚▚▚▚▚▚█▚▚▚▚▚▚▖▖▖▚▚▚▚
        █▜▚▚██▜▚▚▚▜▖▚▚▜▚█▜▚▚▚▜▚▖▚▜▚█▚▚▖▚▚▖▚▚▖▖▚▚
        ▚▚█▚▚▚█▚██▚▚▚▚▚▚▚▚▜▚▚█▜▚▖█▚▚▚▜▚▚▚▚▚▚▜▚█▚█
        █▚▜▚▜▚█▚▜▚▚▜▚█▚▚▚▚▚▚▚▚▚▚▚▖▚▖▚▚▖▚█▚█▚▚▚▖█▚
        ████▚███▚▚▚▚██▚▚▚█▜▚▚▖▚▚▚▖▖▚▚▚▚▚▚▚▚█▚▜▖█
       ▖█▜▚█▚██▜▖▜▜█▜▜█▜▚▚▚▚▚█▖▚▚▚▚█▚▚▚▚▚▚▜▚▚█▖▜
       ▚▖██▚▜▚█▚▚▜▜█▜▜▜██▚▚▚▚█▚▚▚▜▖▚▚█▚▖▚▜▚▚▚▖▚█
       █▚▚▚▚▜▚██▖██▜▚▚█▚▚▖▚▚▜▚▖▚▖▚▚▚▚▚▖▚▚▖▖▖▚▖▚
      ▚▚▚█▚▚▚▚▚█▜▚▚████▚█▚▚▚█▚▖▚▚▚▖▚▚█▚▚▖▚▚▚▖▖▖
      ▚▚▚█▚▚▚▖▖▚▜█▜██▜██▚▚▖██▜▚▜▚█▚▚▚▚▚▚▚▚▖▖▜██
      ▚▚▚▚▜█▚▚▚▚▚█████▚▜██▚██▚▚▚▚▜▚▖▚█▚▚▖▚▖▚▚█
     ▚▚▜▚▚▚▚▜▚▜▚▚▚▚▜▚█▚▜█▚██▚██▚▚▚▚▖▚▚▚▚▖▖▚▚▖█
     ▚▜▚▜▚▚▚▚▚▚█▚▚▚▚▚██▜▜▜███▖▚▚▜█▚▚▖▚█▚▚█▚▖▚
     ▚▜▚▚▚▚▚▚▚▚▚▚▜▜▜▚▚▖▚▖▚▚▜▜██▜▚██▚▚▚▚▚▚▖▜█▚
    ▚▚▖▚▚█▚█▚▚▚█▚▖▚▚▚█▚▚▚▚▚▜██▚█▜▚█▚▜▚▚███▜█▜
    ▚▚▚▜▚▚▚▚▚▚▚▚▚▚▚▖█▚█▚▚▜█▜█▜█▜▚▖▚▚▚██▜▜█▚▜
    ▚▚▚▚▜▚▚▚▚▚▚▜▚▚▚▚▚▚▖▚█▜▖▖█▚▖▜▖▚▖█▚▖█▚▚▜▚█
    ▚▚█▚▚█▚▚▜▚▚▚▚▜▚▚▚▚▚▜▚▖▚█▜█▚▜▜▚█▖▜███▜▚▚
   ▚▚▚▚▚▚▖▜▚█▚▚▚▖▚▚▚▚▚▚▚▚▚▚▚▜█▖▜▜▜█▚▚▚▖▚█▚█
   ▜▚▚▚█▚▖▚█▚█▚▚█▚▚▚▚▚▚▚▖▚▚▚▜▚▚▚▜▚▖▚▖▚▚▚▚▜▚
   ▚▚▚▚▖▚█▖█▜▚▚▚▚▚▚▚▚▖▚▚▖▖█▚▜▚▖▚▚▚▚▖▖▚█▚▚▚
  ▚▚▚▚▚▚▚▚▚█▚▚▚▖▚▚▚█▚▜▚█▚▚▖▜██▚▖▚▚▚▚▚▚▚▚▚▖
  ▚▚▚▚▚▚▚▖▚▚██▚▚▚▚▚▚▚▚▜▚▚█▚██▚▚▚▚▖▚▚▖▚▚█▜▖
  ▚▚▚▚▚▚▚▚▚▚▚▚▚█▚▜▚▚▚▜▚▚▖▚▚▚▚▚▜▚▚▚▚▖▚▚▚▚▚
 ▚██▖▚▚▚▚▚▚▚▚▜▚▚█▚▚▚▚▜▚▚▚▚█▜▖▚▚█▜▜█▜█▚▖▚▖
 ▚▚▚▖▚▚█▚▚▜███▚▚▚▜▚▚▚▚▚█▚▖▖█▖▚████▜███▚██
 ▚█▚▚▚▚██▜▚▜▚▜▜▜█▜▚█▚▜▖▜▚▚▚█▚▜█▚▜▚▚▚▚▚▖▖
    █▜█▚▚▜▚▜▚▜▜▜▚▚▚▚██▖▖▖▚██▖█▚▜▜▚▚▚▚▚▚▖
       ▚█▜▜▜▜▜██▚▜▚▚▚▚▚▚▖▜▚▜▚▚▚▜▚█▚▚▖▖▖
          ██▚▚▚▚▚▚▚▜▚▜▖▚██▜▜▚▖▚▚█▚▚▚▖▜▜
             ▜▚▚▖▚▚▚▖▚▜▜██▜▜▚█▚▚▜▚▚▜██▚
                ▚▚█▚▜▚▚█▖▜▚▚▚▖█▚▚█▚▚█▚
                   █▜▜▚▚▜▜▚▚▚▜█▚▚▚▜█▜█
                      ▚▚▖▚█▖▚▖▜▚▖▚▖▜▚
                         ███▖██▚▖▚▚▚▚
                            ▜▚▚█▚▚▖▖█
                              ▚▖▜█▜▚
                                 ▖█▚

```

Gorgeous.

#### Storage on Azure via HDFS

By adding some additional configuration, you can now use our [HDFS Layer Backend](guide/tile-backends.html#hdfs) to read and write GeoTrellis layers to Microsoft Azure's blob storage.

#### S3 Configurability

[It's now possible to customize how our S3 backend communicates with S3.](guide/examples.html#work-with-s3-using-a-custom-s3client-configuration)

#### Configuring JTS Precision

GeoTrellis uses the [Java Topology Suite](https://github.com/locationtech/jts) for its vector processing. By default, JTS uses a "floating" [PrecisionModel](https://locationtech.github.io/jts/javadoc/org/locationtech/jts/geom/PrecisionModel.html). When writing code that needs to be numerically robust, this default can lead to Topology Exceptions.

You can now use [Typesafe Config](https://github.com/lightbend/config)
to configure this to your application's needs. [See here for the
specifics.](guide/vectors.html#numerical-precision-and-topology-exceptions)

### Added

- [Kerberos authentication is available for properly configured Accumulo clusters](https://github.com/locationtech/geotrellis/pull/2510)
- [Polygonal Summaries for MultibandTiles](https://github.com/locationtech/geotrellis/pull/2374)
- [Filter GeoTiffRDDs by Geometry](https://github.com/locationtech/geotrellis/pull/2409)
- [Can create ValueReaders via URIs through LayerProvides classes](https://github.com/locationtech/geotrellis/pull/2286)
- [Can read/write GeoTiffs with Sinusoidal projections](https://github.com/locationtech/geotrellis/pull/2345)
- [Can Resample via Sum operation](https://github.com/locationtech/geotrellis/pull/2326)

### Changed

- [Negative grid bounds bug](https://github.com/locationtech/geotrellis/pull/2364)
- [getSignedByteArray BugFix - fixes certain read problems](https://github.com/locationtech/geotrellis/pull/2270)
- [Allow Merge Queue To Handle Larger Inputs](https://github.com/locationtech/geotrellis/pull/2400)
- [Generate Windows That Conform To GeoTiff Segments](https://github.com/locationtech/geotrellis/pull/2402)
- [Removed inefficient LayerFilter check](https://github.com/locationtech/geotrellis/pull/2324)
- [Fixed issue with S3 URI not having a key prefix](https://github.com/locationtech/geotrellis/pull/2316)
- [Improve S3 makePath function](https://github.com/locationtech/geotrellis/pull/2352)
- [Fix S3GeoTiffRDD behavior with some options.](https://github.com/locationtech/geotrellis/pull/2317)
- [Allow Contains(Point) query for temporal rdds](https://github.com/locationtech/geotrellis/pull/2297)
- [Haversine formula fix](https://github.com/locationtech/geotrellis/pull/2408)
- [Use Scaffeine instead of LRU cache in HadoopValueReader](https://github.com/locationtech/geotrellis/pull/2421)
- [Fix GeoTiffInfo serialization issues](https://github.com/locationtech/geotrellis/pull/2312)
- [Estimate partitions number based on GeoTiff segments](https://github.com/locationtech/geotrellis/pull/2296)
- [Estimate partitions number basing on a desired partition size](https://github.com/locationtech/geotrellis/pull/2289)
- [Pyramid operation preserves partitioning](https://github.com/locationtech/geotrellis/pull/2311)
- [Don't constrain GridBounds size to IntMax x IntMax](https://github.com/locationtech/geotrellis/pull/2292)
- [4-Connected Line Drawing](https://github.com/locationtech/geotrellis/pull/2336)
- [Added requirement for CRS implementations to provide a readable toString representation.](https://github.com/locationtech/geotrellis/pull/2337)
- [Allow rasterizer to store Z value at double precision](https://github.com/locationtech/geotrellis/pull/2388)
- [Changed scheme path file from /User -> current working dir](https://github.com/locationtech/geotrellis/pull/2393)
- [Fix CRS parser and proj4 `cea` projection support](https://github.com/locationtech/geotrellis/pull/2403)

## [1.1.0] - 2017-06-22

### Added

- [Spark Enabled Cost Distance](https://github.com/locationtech/geotrellis/pull/1999)
- [Conforming Delaunay Triangulation](https://github.com/locationtech/geotrellis/pull/1848)
- Added a fractional-pixel rasterizer for [polygons](https://github.com/locationtech/geotrellis/pull/1873) and [multipolygons](https://github.com/locationtech/geotrellis/pull/1894)
- [Added collections API mapalgebra local and masking functions](https://github.com/locationtech/geotrellis/pull/1947)
- [Added withDefaultNoData method for CellTypes](https://github.com/locationtech/geotrellis/pull/1966)
- [Moved Spark TestEnvironment to spark-testkit subproject for usage outside of GeoTrellis](https://github.com/locationtech/geotrellis/issues/2012)
- [Add convenience overloads to GeoTiff companion object](https://github.com/locationtech/geotrellis/pull/1840)
- [Added matplotlib's Magma, Inferno, Plasma, and Viridis color ramps](https://github.com/locationtech/geotrellis/pull/2053)
- [Added library of land use classification color maps.](https://github.com/locationtech/geotrellis/pull/2073)
- [Add MGRS encode/decode support to proj4](https://github.com/locationtech/geotrellis/pull/1838)
- [Rasters write support to HDFS / S3](https://github.com/locationtech/geotrellis/pull/2102)
- [Added Range-based reading of HTTP resources](https://github.com/locationtech/geotrellis/pull/2067)
- [Improved the WKT parser that powers the WKT.getEpsgCode method](https://github.com/locationtech/geotrellis/pull/1931)
- [Updated the geotrellis-geowave subproject to GeoWave 0.9.3](https://github.com/locationtech/geotrellis/pull/1933)
- [Updated the geotrellis-geomesa subproject to GeoMesa 1.2.7](https://github.com/locationtech/geotrellis/pull/1930)
- [Use H3 rather than Next Fit when building S3 partitions from paths](https://github.com/locationtech/geotrellis/pull/1956)
- [Added delimiter option to S3InputFormat and S3GeoTiffRDD.](https://github.com/locationtech/geotrellis/pull/2062)
- [Signed S3 Streaming for GeoTiff reader (HTTP with GET instead of HEAD request)](https://github.com/locationtech/geotrellis/pull/2091)
- [Relaxed constraints to improve layer deletion capabilities](https://github.com/locationtech/geotrellis/pull/2039)
- [Allow HadoopGeoTiffRDD and S3GeoTiffRDD to maintain additional key information such as file name](https://github.com/locationtech/geotrellis/pull/2050)
- [Added API sugar for simplifying construction of AvroRecordCodec](https://github.com/locationtech/geotrellis/pull/2030)
- [Make compression optional for Avro encoding and decoding](https://github.com/locationtech/geotrellis/pull/1952/files)
- [Optimization to avoid unspecialized Function3 usage in Hillshade, Slope and Aspect](https://github.com/locationtech/geotrellis/pull/2049/files)
- [Updated multiple dependencies](https://github.com/locationtech/geotrellis/pull/1945)
- [Upgraded ScalaPB version for VectorTile](https://github.com/locationtech/geotrellis/pull/2038)
- Added Avro codecs for [ProjectedExtent and TemporalProjectedExtent](https://github.com/locationtech/geotrellis/pull/1971) and [ConstantTile types](https://github.com/locationtech/geotrellis/pull/2015)
- [Repartition in ETL when re-tiling increases layer resolution](https://github.com/locationtech/geotrellis/pull/2135)
- [In GeoTiff reader, compute CellSize from TIFF tags](https://github.com/locationtech/geotrellis/pull/1996)
- [Improved apply methods for constructing S3RangeReader](https://github.com/locationtech/geotrellis/pull/1994)
- [Reorganized handling of CellType.name](https://github.com/locationtech/geotrellis/pull/2142)
- Documentation improvements, including [porting the docts to reStructuredText](https://github.com/locationtech/geotrellis/pull/2016)
- [Added top-level "Sinusoidal" CRS, commonly used with MODIS](https://github.com/locationtech/geotrellis/pull/2145)
- [Added conditional to key bounds decomposition to detect full bounds query in Acccumulo.](https://github.com/locationtech/geotrellis/pull/2164)
- [Support for the ability to specify output CRS via proj4 string.](https://github.com/locationtech/geotrellis/pull/2169)

### Changes

- [Fixed issues that made GeoTiff streaming off of S3 slow and broken](https://github.com/locationtech/geotrellis/pull/1905)
- [Give a better error message for CRS write failures](https://github.com/locationtech/geotrellis/pull/1874)
- [Fix clipping logic during polygon layer query](https://github.com/locationtech/geotrellis/pull/2213)
- [Fixed type for CRS authority in NAD83](https://github.com/locationtech/geotrellis/pull/1916)
- [Moved JsonFormats for CellSize and CellType to their proper place](https://github.com/locationtech/geotrellis/pull/1919)
- [Fixed polygon rasterization for complex polygon test cases](https://github.com/locationtech/geotrellis/pull/1963)
- [Fixed issue with FileLayerDeleter](https://github.com/locationtech/geotrellis/pull/2015)
- [Fixed issue with logger serialization](https://github.com/locationtech/geotrellis/pull/2017)
- [Fixed bug in renderPng that caused incorrect rendering of non-floating-point rasters](https://github.com/locationtech/geotrellis/issues/2022) - [Don't allow illegal TileLayouts](https://github.com/locationtech/geotrellis/issues/2026)
- [Prevent error from happening during Pyramiding](https://github.com/locationtech/geotrellis/pull/2029)
- [Ensure tile columns are not zero when rounding](https://github.com/locationtech/geotrellis/pull/2031)
- [Fixed malformed XML error that was happening after failed S3 ingest](https://github.com/locationtech/geotrellis/pull/2040)
- [Fix issue with S3LayerDeleter deleting files outside of layer](https://github.com/locationtech/geotrellis/pull/2070)
- [Fix TemporalProjectedExtentCodec to handling proj4 strings when CRS isn't available](https://github.com/locationtech/geotrellis/pull/2034)
- [Fixed layoutForZoom to allow 0 zoom level](https://github.com/locationtech/geotrellis/pull/2057)
- [Fixed MapKeyTransform to deal with points north and west of extent](https://github.com/locationtech/geotrellis/pull/2060)
- [Fixed GeoTiff reading for GeoTiffs with model tie point and PixelIsPoint](https://github.com/locationtech/geotrellis/pull/2061)
- [Fixed issue with reading tiny (4 pixel or less) GeoTiffs](https://github.com/locationtech/geotrellis/pull/2063)
- [Fix usage of IntCachedColorMap in Indexed PNG encoding](https://github.com/locationtech/geotrellis/pull/2075)
- [Ensure keyspace exists in CassandraRDDWriter](https://github.com/locationtech/geotrellis/pull/2083)
- [Resolved repartitioning issue with HadoopGeoTiffRDD](https://github.com/locationtech/geotrellis/pull/2105)
- [Fixed schema for intConstantTileCodec](https://github.com/locationtech/geotrellis/pull/2110)
- [In HadoopAttributeStore, get absolute path for attributePath](https://github.com/locationtech/geotrellis/pull/2123)
- [In AccumuloLayerDeleter, close batch deleter](https://github.com/locationtech/geotrellis/pull/2117)
- [S3InputFormat - bucket names support period and dashes](https://github.com/locationtech/geotrellis/pull/2133)
- [Fix TMS scheme min zoom level](https://github.com/locationtech/geotrellis/pull/2137)
- [S3AttributeStore now handles ending slashes in prefix.](https://github.com/locationtech/geotrellis/pull/2147)
- [Cell type NoData logic for unsigned byte / short not working properly](https://github.com/locationtech/geotrellis/pull/2171)
- [CellSize values should not be truncated to integer when parsing from Json.](https://github.com/locationtech/geotrellis/pull/2174)
- [Fixes to GeoTiff writing with original LZW compression.](https://github.com/locationtech/geotrellis/pull/2180)
- [In ArrayTile.convert, debug instead of warn against floating point data loss.](https://github.com/locationtech/geotrellis/pull/2190)
- [Fixes incorrect metadata update in a per-tile reprojection case](https://github.com/locationtech/geotrellis/pull/2201)
- [Fix issue with duplicate tiles being read for File and Cassandra backends](https://github.com/locationtech/geotrellis/pull/2200)
- [Move to a different Json Schema validator](https://github.com/locationtech/geotrellis/pull/2222)
- [S3InputFormat does not filter according to extensions when partitionCount is used](https://github.com/locationtech/geotrellis/issues/2231)
- [In S3GeoTiffReader, partitionBytes has no effect if maxTileSize is set](https://github.com/locationtech/geotrellis/issues/2232)
- [Fixes typos with rasterizer extension methods](https://github.com/locationtech/geotrellis/pull/2245)
- [Fix writing multiband GeoTiff with compression](https://github.com/locationtech/geotrellis/pull/2246)
- [Fixed issue with BigTiff vs non-BigTiff offset value packing](https://github.com/locationtech/geotrellis/pull/2247)

While we are trying to stick strictly to [SemVer](http://semver.org/), there are slight API changes in this release. We felt that while this does break SemVer in the strictest sense, the change were not enough to warrant a 2.0 release. Our hope is in the future to be more cognizant of API changes for future releases.

- Made EPSG capitalization [consistent in method names](https://github.com/locationtech/geotrellis/commit/343588b4b066851ea6b35a7d9cc671f4a6d47f2c):
  - In `geotrellis.proj4.CRS`, changed `getEPSGCode` to `getEpsgCode`
  - In `geotrellis.proj4.io.wkt.WKT`, changed `fromEPSGCode` to `fromEpsgCode` and `getEPSGCode` to `getEpsgCode`

- Changed some internal but publicly visible [classes dealing with GeoTiff reading](https://github.com/locationtech/geotrellis/pull/1905)
  - Changed `size` to `length` in `ArraySegmentBytes`
  - Replaced `foreach` on SegmentBytes with `getSegments`, which the caller can iterate over themselves
  - Changed `getDecompressedBytes` to `decompressGeoTiffSegment`

- Changed some internal but publicly visible [implicit classes and read methods around TiffTagReader](https://github.com/locationtech/geotrellis/pull/2247)
  - Added as an implicit parameter to multiple locations, most publicly in `TiffTagReader.read(byteReader: ByteReader, tagsStartPosition: Long)(implicit ttos: TiffTagOffsetSize)`. Also changed that method from being generic to always taking a `Long` offset.

- Moved some misplaced [implicit `JsonFormats`](https://github.com/locationtech/geotrellis/pull/1919)
  - Moved `CellTypeFormat` and `CellSizeFormat` from `geotrellis.spark.etl.config.json` in the `spark-etl` subproject to `geotrellis.raster.io.json.Implicits` in the `raster` subproject.

- Changed LazyLogger [from the `com.typesafe.scalalogging` version to our own version](https://github.com/locationtech/geotrellis/pull/2017)
  - This shouldn't break any code, but technically is an API change.

## [1.0.0] - 2017-05-17

### Added

- GeoTools support
  - Add Support for GeoTools SimpleFeature [#1495](https://github.com/locationtech/geotrellis/pull/1495)
  - Conversions between GeoTools GridCoverage2D and GeoTrellis Raster types [#1502](https://github.com/locationtech/geotrellis/pull/1502)
- Streaming GeoTiff reading [#1559](https://github.com/locationtech/geotrellis/pull/1559)
- Windowed GeoTiff ingests into GeoTrellis layers, allowing users to ingest large GeoTiffs
  [#1763](https://github.com/locationtech/geotrellis/pull/1763)
  - Reading TiffTags via MappedByteBuffer [#1541](https://github.com/locationtech/geotrellis/pull/1541)
  - Cropped Windowed GeoTiff Reading [#1559](https://github.com/locationtech/geotrellis/pull/1559)
  - Added documentation to the GeoTiff* files [#1560](https://github.com/locationtech/geotrellis/pull/1560)
  - Windowed GeoTiff Docs [#1616](https://github.com/locationtech/geotrellis/pull/1616)
- GeoWave Raster/Vector support (experimental)
  - Create GeoWave Subproject [#1542](https://github.com/locationtech/geotrellis/pull/1542)
  - Add vector capabilities to GeoWave support [#1581](https://github.com/locationtech/geotrellis/pull/1581)
  - Fix GeoWave Tests [#1665](https://github.com/locationtech/geotrellis/pull/1665)
- GeoMesa Vector support (experimental)
  - Create GeoMesa subproject [#1621](https://github.com/locationtech/geotrellis/pull/1621)
- Moved to a JSON-configuration ETL process
  - ETL Refactor [#1553](https://github.com/locationtech/geotrellis/pull/1553)
  - ETL Improvements and other issues fixes [#1647](https://github.com/locationtech/geotrellis/pull/1647)
- Vector Tile reading and writing, file-based and as GeoTrellis layers in RDDs. [#1622](https://github.com/locationtech/geotrellis/pull/1622)
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
- `CollectNeighbors` feature, allowing users to group arbitrary values by the neighbor keys according to their SpatialComponent [#1860](https://github.com/locationtech/geotrellis/pull/1860)
- **Documentation:** We moved to ReadTheDocs, and put a lot of work into making our docs significantly better. [See them here.](http://geotrellis.readthedocs.io/en/1.0/)

### Minor Additions

- Documentation improvements: Quickstart,  Examples
  - Added example for translating from `SpaceTimeKey` to `SpatialKey` [#1549](https://github.com/locationtech/geotrellis/pull/1549)
  - doc-examples subproject; example for tiling to GeoTiff [#1564](https://github.com/locationtech/geotrellis/pull/1564)
  - Added example for focal operation on multiband layer. [#1577](https://github.com/locationtech/geotrellis/pull/1577)
  - Projections, Extents, and Layout Definitions doc [#1608](https://github.com/locationtech/geotrellis/pull/1608)
  - Added example of turning a list of features into GeoJSON [#1609](https://github.com/locationtech/geotrellis/pull/1609)
  - Example: `ShardingKeyIndex[K]` [#1633](https://github.com/locationtech/geotrellis/pull/1633)
  - Example: `VoxelKey` [#1639](https://github.com/locationtech/geotrellis/pull/1639)
- Introduce ADR concept
  - ADR: HDFS Raster Layers [#1582](https://github.com/locationtech/geotrellis/pull/1582)
  - ADR: Readers / Writers Multithreading [#1613](https://github.com/locationtech/geotrellis/pull/1613)
- Fixed some markdown docs [#1625](https://github.com/locationtech/geotrellis/pull/1625)
- `parseGeoJson` lives in `geotrellis.vector.io` [#1649](https://github.com/locationtech/geotrellis/pull/1649)
- Parallelize reads for S3, File, and Cassandra backends [#1607](https://github.com/locationtech/geotrellis/pull/1607)
- Kernel Density in Spark
- k-Nearest Neighbors
- Updated slick
- Added GeoTiff read/write support of `TIFFTAG_PHOTOMETRIC` via `GeoTiffOptions`. [#1667](https://github.com/locationtech/geotrellis/pull/1667)
- Added ability to read/write color tables for GeoTIFFs encoded with palette photometric interpretation [#1802](https://github.com/locationtech/geotrellis/pull/1802)
- Added `ColorMap` to String conversion [#1512](https://github.com/locationtech/geotrellis/pull/1512)
- Add split by cols/rows to SplitMethods [#1538](https://github.com/locationtech/geotrellis/pull/1538)
- Improved HDFS support [#1556](https://github.com/locationtech/geotrellis/pull/1556)
- Added Vector Join operation for Spark [#1610](https://github.com/locationtech/geotrellis/pull/1610)
- Added Histograms Over Fractions of RDDs of Tiles [#1692](https://github.com/locationtech/geotrellis/pull/1692)
- Add `interpretAs` and `withNoData` methods to Tile [#1702](https://github.com/locationtech/geotrellis/pull/1702)
- Changed GeoTiff reader to handle BigTiff [#1753](https://github.com/locationtech/geotrellis/pull/1753)
- Added `BreakMap` for reclassification based on range values. [#1760](https://github.com/locationtech/geotrellis/pull/1760)
- Allow custom save actions on ETL [#1764](https://github.com/locationtech/geotrellis/pull/1764)
- Multiband histogram methods [#1784](https://github.com/locationtech/geotrellis/pull/1784)
- `DelayedConvert` feature, allowing users to delay conversions on tiles until a map or combine operation, so that tiles are not iterated over unnecessarily [#1797](https://github.com/locationtech/geotrellis/pull/1797)
- Add convenience overloads to GeoTiff companion object [#1840](https://github.com/locationtech/geotrellis/pull/1840)

### Fixes / Optimizations

- Fixed GeoTiff bug in reading NoData value if len = 4 [#1490](https://github.com/locationtech/geotrellis/pull/1490)
  - Add detail to avro exception message [#1505](https://github.com/locationtech/geotrellis/pull/1505)
  - Fix: The `toSpatial` method gives metadata of type `TileLayerMetadata[SpaceTimeKey]`
    - Custom `Functor` Typeclass [#1643](https://github.com/locationtech/geotrellis/pull/1643)
  - Allow `Intersects(polygon: Polygon)` in layer query [#1644](https://github.com/locationtech/geotrellis/pull/1644)
  - Optimize `ColorMap` [#1648](https://github.com/locationtech/geotrellis/pull/1648)
  - Make regex for s3 URLs handle s3/s3a/s3n [#1652](https://github.com/locationtech/geotrellis/pull/1652)
  - Fixed metadata handling on surface calculation for tile layer RDDs [#1684](https://github.com/locationtech/geotrellis/pull/1684)
  - Fixed reading GeoJson with 3d values [#1704](https://github.com/locationtech/geotrellis/pull/1704)
  - Fix to Bicubic Interpolation [#1708](https://github.com/locationtech/geotrellis/pull/1708)
  - Fixed: Band tags with values of length > 31 have additional white space added to them [#1756](https://github.com/locationtech/geotrellis/pull/1756)
  - Fixed NoData bug in tile merging logic [#1793](https://github.com/locationtech/geotrellis/pull/1793)
  - Fixed Non-Point Pixel + Partial Cell Rasterizer Bug [#1804](https://github.com/locationtech/geotrellis/pull/1804)

### New Committers

- metasim
- lokifacio
- aeffrig
- jpolchlo
- jbouffard
- vsimko
- longcmu
- miafg

## [0.10.3]

- [PR #1611](https://github.com/geotrellis/geotrellis/pull/1611) Any `RDD` of `Tile`s can utilize Polygonal Summary methods. (@fosskers)
- [PR #1573](https://github.com/geotrellis/geotrellis/pull/1573) New `foreach` for `MultibandTile` which maps over each band at once. (@hjaekel)
- [PR #1600](https://github.com/geotrellis/geotrellis/pull/1600) New `mapBands` method to map more cleanly over the bands of a `MultibandTile`.

## [0.10.2]

- [PR #1561](https://github.com/geotrellis/geotrellis/pull/1561) Fix to polygon sequence union, account that it can result in NoResult. (1)
- [PR #1585](https://github.com/geotrellis/geotrellis/pull/1585) Removed warnings; add proper subtyping to GetComponent and SetComponent identity implicits; fix jai travis breakage. (1)
- [PR #1569](https://github.com/geotrellis/geotrellis/pull/1569) Moved RDDLayoutMergeMethods functionality to object. (1)
- [PR #1494](https://github.com/geotrellis/geotrellis/pull/1494) Add ETL option to specify upper zoom limit for raster layer ingestion (@mbertrand)
- [PR #1571](https://github.com/geotrellis/geotrellis/pull/1571) Fix scallop upgrade issue in spark-etl (@pomadchin)
- [PR #1543](https://github.com/geotrellis/geotrellis/pull/1543) Fix to Hadoop LayerMover (@pomadchin)

Special thanks to new contributor @mbertrand!

## [0.10.1]

- PR #1451 Optimize reading from compressed Bit geotiffs (@shiraeeshi)
- PR #1454 Fix issues with IDW interpolation (@lokifacio)
- PR #1457 Store FastMapHistogram counts as longs (@jpolchlo)
- PR #1460 Fixes to user defined float/double CellType parsing (@echeipesh)
- PR #1461 Pass resampling method argument to merge in CutTiles (1)
- PR #1466 Handle Special Characters in proj4j (@jamesmcclain)
- PR #1468 Fix nodata values in codecs (@shiraeeshi)
- PR #1472 Fix typo in MultibandIngest.scala (@timothymschier)
- PR #1478 Fix month and year calculations (@shiraeeshi)
- PR #1483 Fix Rasterizer Bug (@jamesmcclain)
- PR #1485 Upgrade dependencies as part of our LocationTech CQ process (1)
- PR #1487 Handle entire layers of NODATA (@fosskers)
- PR #1493 Added support for int32raw cell types in
  CellType.fromString (@jpolchlo)
- PR #1496 Update slick (@adamkozuch, @moradology)
- PR #1498 Add ability to specify number of streaming buckets (@moradology)
- PR #1500 Add logic to ensure use of minval/avoid repetition of breaks (@moradology)
- PR #1501 SparkContext temporal GeoTiff format args (@echeipesh)
- PR #1510 Remove dep on cellType when specifying layoutExtent (@fosskers)
- PR #1529 LayerUpdater fix (@pomadchin)

Special thanks to new contributors @fosskers, @adamkozuch, @jpolchlo, @shiraeeshi, @lokifacio!

## [0.10.0]

The long awaited GeoTrellis 0.10 release is here!

It’s been a while since the 0.9 release of GeoTrellis, and there are
many significant changes and improvements in this release. GeoTrellis
has become an expansive suite of modular components that aide users in
the building of geospatial application in Scala, and as always we’ve
focused specifically on high performance and distributed computing. This
is the first official release that supports working with Apache Spark,
and we are very pleased with the results that have come out of the
decision to support Spark as our main distributed processing engine.
Those of you who have been tuned in for a while know we started with a
custom built processing engine based on Akka actors; this original
execution engine still exists in 0.10 but is in a deprecated state in
the geotrellis-engine subproject. Along with upgrading GeoTrellis to
support Spark and handle arbitrarily-sized raster data sets, we’ve been
making improvements and additions to core functionality, including
adding vector and projection support.

It’s been long enough that release notes, stating what has changed since
0.9, would be quite unwieldy. Instead I put together a list of features
that GeoTrellis 0.10 supports. This is included in the README on the
GeoTrellis Github, but I will put them here as well. It is organized by
subproject, with more basic and core subprojects higher in the list, and
the subprojects that rely on that core functionality later in the list,
along with a high level description of each subproject.

### geotrellis-proj4

- Represent a Coordinate Reference System (CRS) based on Ellipsoid,
  Datum, and Projection.
- Translate CRSs to and from proj4 string representations.
- Lookup CRS's based on EPSG and other codes.
- Transform `(x, y)` coordinates from one CRS to another.

### geotrellis-vector

- Provides a scala idiomatic wrapper around JTS types: Point, Line
  (LineString in JTS), Polygon, MultiPoint, MultiLine (MultiLineString
  in JTS), MultiPolygon, GeometryCollection
- Methods for geometric operations supported in JTS, with results that
  provide a type-safe way to match over possible results of
  geometries.
- Provides a Feature type that is the composition of a geometry and a
  generic data type.
- Read and write geometries and features to and from GeoJSON.
- Read and write geometries to and from WKT and WKB.
- Reproject geometries between two CRSs.
- Geometric operations: Convex Hull, Densification, Simplification
- Perform Kriging interpolation on point values.
- Perform affine transformations of geometries

### geotrellis-vector-testkit

- GeometryBuilder for building test geometries
- GeometryMatcher for scalatest unit tests, which aides in testing
  equality in geometries with an optional threshold.

### geotrellis-raster

- Provides types to represent single- and multi-band rasters,
  supporting Bit, Byte, UByte, Short, UShort, Int, Float, and Double
  data, with either a constant NoData value (which improves
  performance) or a user defined NoData value.
- Treat a tile as a collection of values, by calling "map" and
  "foreach", along with floating point valued versions of those
  methods (separated out for performance).
- Combine raster data in generic ways.
- Render rasters via color ramps and color maps to PNG and JPG images.
- Read GeoTiffs with DEFLATE, LZW, and PackBits compression, including
  horizontal and floating point prediction for LZW and DEFLATE.
- Write GeoTiffs with DEFLATE or no compression.
- Reproject rasters from one CRS to another.
- Resample of raster data.
- Mask and Crop rasters.
- Split rasters into smaller tiles, and stitch tiles into larger
  rasters.
- Derive histograms from rasters in order to represent the
  distribution of values and create quantile breaks.
- Local Map Algebra operations: Abs, Acos, Add, And, Asin, Atan,
  Atan2, Ceil, Cos, Cosh, Defined, Divide, Equal, Floor, Greater,
  GreaterOrEqual, InverseMask, Less, LessOrEqual, Log, Majority, Mask,
  Max, MaxN, Mean, Min, MinN, Minority, Multiply, Negate, Not, Or,
  Pow, Round, Sin, Sinh, Sqrt, Subtract, Tan, Tanh, Undefined,
  Unequal, Variance, Variety, Xor, If
- Focal Map Algebra operations: Hillshade, Aspect, Slope, Convolve,
  Conway's Game of Life, Max, Mean, Median, Mode, Min, MoransI,
  StandardDeviation, Sum
- Zonal Map Algebra operations: ZonalHistogram, ZonalPercentage
- Operations that summarize raster data intersecting polygons: Min,
  Mean, Max, Sum.
- Cost distance operation based on a set of starting points and a
  friction raster.
- Hydrology operations: Accumulation, Fill, and FlowDirection.
- Rasterization of geometries and the ability to iterate over cell
  values covered by geometries.
- Vectorization of raster data.
- Kriging Interpolation of point data into rasters.
- Viewshed operation.
- RegionGroup operation.

### geotrellis-raster-testkit

- Build test raster data.
- Assert raster data matches Array data or other rasters in scalatest.

### geotrellis-spark

- Generic way to represent key value RDDs as layers, where the key
  represents a coordinate in space based on some uniform grid layout,
  optionally with a temporal component.
- Represent spatial or spatiotemporal raster data as an RDD of raster
  tiles.
- Generic architecture for saving/loading layers RDD data and metadata
  to/from various backends, using Spark's IO API with Space Filling
  Curve indexing to optimize storage retrieval (support for Hilbert
  curve and Z order curve SFCs). HDFS and local file system are
  supported backends by default, S3 and Accumulo are supported
  backends by the `geotrellis-s3` and `geotrellis-accumulo` projects,
  respectively.
- Query architecture that allows for simple querying of layer data by
  spatial or spatiotemporal bounds.
- Perform map algebra operations on layers of raster data, including
  all supported Map Algebra operations mentioned in the
  geotrellis-raster feature list.
- Perform seamless reprojection on raster layers, using neighboring
  tile information in the reprojection to avoid unwanted NoData cells.
- Pyramid up layers through zoom levels using various resampling
  methods.
- Types to reason about tiled raster layouts in various CRS's and
  schemes.
- Perform operations on raster RDD layers: crop, filter, join, mask,
  merge, partition, pyramid, render, resample, split, stitch, and
  tile.
- Polygonal summary over raster layers: Min, Mean, Max, Sum.
- Save spatially keyed RDDs of byte arrays to z/x/y files into HDFS or
  the local file system. Useful for saving PNGs off for use as map
  layers in web maps or for accessing GeoTiffs through z/x/y tile
  coordinates.
- Utilities around creating spark contexts for applications using
  GeoTrellis, including a Kryo registrator that registers most types.

### geotrellis-spark-testkit

- Utility code to create test RDDs of raster data.
- Matching methods to test equality of RDDs of raster data in
  scalatest unit tests.

### geotrellis-accumulo

- Save and load layers to and from Accumulo. Query large layers
  efficiently using the layer query API.

### geotrellis-cassandra

Save and load layers to and from Casandra. Query large layers
efficiently using the layer query API.

### geotrellis-s3

- Save/load raster layers to/from the local filesystem or HDFS using
  Spark's IO API.
- Save spatially keyed RDDs of byte arrays to z/x/y files in S3.
  Useful for saving PNGs off for use as map layers in web maps.

### geotrellis-etl

- Parse command line options for input and output of ETL (Extract, Transform, and Load) applications
- Utility methods that make ETL applications easier for the user to
  build.
- Work with input rasters from the local file system, HDFS, or S3
- Reproject input rasters using a per-tile reproject or a seamless
  reprojection that takes into account neighboring tiles.
- Transform input rasters into layers based on a ZXY layout scheme
- Save layers into Accumulo, S3, HDFS or the local file system.

### geotrellis-shapefile

- Read geometry and feature data from shapefiles into GeoTrellis types using GeoTools.

### geotrellis-slick

- Save and load geometry and feature data to and from PostGIS using
  the slick scala database library.
- Perform PostGIS `ST_` operations in PostGIS through scala.

[Unreleased]: https://github.com/locationtech/geotrellis/compare/v3.7.0...HEAD
[3.7.0]: https://github.com/locationtech/geotrellis/compare/v3.6.3...v3.7.0
[3.6.3]: https://github.com/locationtech/geotrellis/compare/v3.6.2...v3.6.3
[3.6.2]: https://github.com/locationtech/geotrellis/compare/v3.6.1...v3.6.2
[3.6.1]: https://github.com/locationtech/geotrellis/compare/v3.6.0...v3.6.1
[3.6.0]: https://github.com/locationtech/geotrellis/compare/v3.5.2...v3.6.0
[3.5.2]: https://github.com/locationtech/geotrellis/compare/v3.5.1...v3.5.2
[3.5.1]: https://github.com/locationtech/geotrellis/compare/v3.5.0...v3.5.1
[3.5.0]: https://github.com/locationtech/geotrellis/compare/v3.4.1...v3.5.0
[3.4.1]: https://github.com/locationtech/geotrellis/compare/v3.4.0...v3.4.1
[3.4.0]: https://github.com/locationtech/geotrellis/compare/v3.3.0...v3.4.0
[3.3.0]: https://github.com/locationtech/geotrellis/compare/v3.2.0...v3.3.0
[3.2.0]: https://github.com/locationtech/geotrellis/compare/v3.1.0...v3.2.0
[3.1.0]: https://github.com/locationtech/geotrellis/compare/v3.0.0...v3.1.0
[3.0.0]: https://github.com/locationtech/geotrellis/compare/v2.3.0...v3.0.0
[2.3.0]: https://github.com/locationtech/geotrellis/compare/v2.2.0...v2.3.0
[2.2.0]: https://github.com/locationtech/geotrellis/compare/v2.1.0...v2.2.0
[2.1.0]: https://github.com/locationtech/geotrellis/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/locationtech/geotrellis/compare/v1.2.1...v2.0.0
[1.2.1]: https://github.com/locationtech/geotrellis/compare/v1.2.0...v1.2.1
[1.2.1]: https://github.com/locationtech/geotrellis/compare/v1.2.0...v1.2.1
[1.2.0]: https://github.com/locationtech/geotrellis/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/locationtech/geotrellis/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/locationtech/geotrellis/compare/v0.10.3...v1.0.0
[0.10.3]: https://github.com/locationtech/geotrellis/compare/v0.10.2...v0.10.3
[0.10.2]: https://github.com/locationtech/geotrellis/compare/v0.10.1...v0.10.2
[0.10.1]: https://github.com/locationtech/geotrellis/compare/v0.10.0...v0.10.1
[0.10.0]: https://github.com/locationtech/geotrellis/compare/v0.9.0...v0.10.0
