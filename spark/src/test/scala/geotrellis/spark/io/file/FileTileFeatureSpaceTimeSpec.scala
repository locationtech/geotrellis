package geotrellis.spark.io.file

import geotrellis.raster.{Tile, TileFeature}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestTileFeatureFiles

import com.github.nscala_time.time.Imports._


class FileTileFeatureSpaceTimeSpec
    extends PersistenceSpec[SpaceTimeKey, TileFeature[Tile, Tile], TileLayerMetadata[SpaceTimeKey]]
    with SpaceTimeKeyIndexMethods
    with TestEnvironment
    with TestTileFeatureFiles
    with CoordinateSpaceTimeTileFeatureSpec
    with LayerUpdateSpaceTimeTileFeatureSpec {
  lazy val reader = FileLayerReader(outputLocalPath)
  lazy val writer = FileLayerWriter(outputLocalPath)
  lazy val deleter = FileLayerDeleter(outputLocalPath)
  lazy val copier = FileLayerCopier(outputLocalPath)
  lazy val mover  = FileLayerMover(outputLocalPath)
  lazy val reindexer = FileLayerReindexer(outputLocalPath)
  lazy val updater = FileLayerUpdater(outputLocalPath)
  lazy val tiles = FileValueReader(outputLocalPath)
  lazy val sample =  CoordinateSpaceTime
}
