package geotrellis.spark.io.hadoop

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles

import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime

class HadoopGridTimeKeySpec
  extends PersistenceSpec[GridTimeKey, Tile, RasterMetadata[GridTimeKey]]
    with GridTimeKeyIndexMethods
    with TestEnvironment
    with TestFiles
    with CoordinateGridTimeKeyTests
    with LayerUpdateGridTimeKeyTileTests {
  lazy val reader = HadoopLayerReader(outputLocal)
  lazy val writer = HadoopLayerWriter(outputLocal)
  lazy val deleter = HadoopLayerDeleter(outputLocal)
  lazy val copier = HadoopLayerCopier(outputLocal)
  lazy val mover  = HadoopLayerMover(outputLocal)
  lazy val reindexer = HadoopLayerReindexer(outputLocal)
  lazy val updater = HadoopLayerUpdater(outputLocal)
  lazy val tiles = HadoopTileReader[GridTimeKey, Tile](outputLocal)
  lazy val sample =  CoordinateSpaceTime
}
