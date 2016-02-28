package geotrellis.spark.io.file

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import geotrellis.spark.io.avro.codecs._

class FileSpatialSpec
    extends PersistenceSpec[SpatialKey, Tile, RasterMetaData]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with TestFiles
    with AllOnesTestTileTests {
  lazy val reader = FileLayerReader[SpatialKey, Tile, RasterMetaData](outputLocalPath)
  lazy val writer = FileLayerWriter(outputLocalPath)
  lazy val deleter = FileLayerDeleter[SpatialKey, Tile, RasterMetaData](outputLocalPath)
  lazy val copier = FileLayerCopier[SpatialKey, Tile, RasterMetaData](outputLocalPath)
  lazy val mover  = FileLayerMover[SpatialKey, Tile, RasterMetaData](outputLocalPath)
  lazy val reindexer = FileLayerReindexer[SpatialKey, Tile, RasterMetaData](outputLocalPath, ZCurveKeyIndexMethod)
  lazy val tiles = FileTileReader[SpatialKey, Tile](outputLocalPath)
  lazy val sample = AllOnesTestFile
}
