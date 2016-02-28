package geotrellis.spark.io.file

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import geotrellis.spark.io.avro.codecs._
import com.github.nscala_time.time.Imports._

class FileSpaceTimeSpec
    extends PersistenceSpec[SpaceTimeKey, Tile, RasterMetaData]
    with SpaceTimeKeyIndexMethods
    with TestEnvironment
    with TestFiles
    with CoordinateSpaceTimeTests {
  lazy val reader = FileLayerReader[SpaceTimeKey, Tile, RasterMetaData](outputLocalPath)
  lazy val writer = FileLayerWriter(outputLocalPath)
  lazy val deleter = FileLayerDeleter[SpaceTimeKey, Tile, RasterMetaData](outputLocalPath)
  lazy val copier = FileLayerCopier[SpaceTimeKey, Tile, RasterMetaData](outputLocalPath)
  lazy val mover  = FileLayerMover[SpaceTimeKey, Tile, RasterMetaData](outputLocalPath)
  lazy val reindexer = FileLayerReindexer[SpaceTimeKey, Tile, RasterMetaData](outputLocalPath, ZCurveKeyIndexMethod.byPattern("YMM"))
  lazy val tiles = FileTileReader[SpaceTimeKey, Tile](outputLocalPath)
  lazy val sample =  CoordinateSpaceTime
}
