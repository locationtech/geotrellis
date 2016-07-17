GeoTrellis 0.10.1, a bugfix release of the 0.10 series, is released. Here are the changes that are included:

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

Special thanks to new contributors @fosskers, @adamkozuch, @jpolchlo, @shiraeeshi, @lokifacio!
