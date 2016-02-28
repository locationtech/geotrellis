package geotrellis.spark.io.hadoop

import com.github.nscala_time.time.Imports._
import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import geotrellis.spark.io.avro.codecs._
import org.joda.time.DateTime

class HadoopSpaceTimeSpec
  extends PersistenceSpec[SpaceTimeKey, Tile, RasterMetaData]
    with SpaceTimeKeyIndexMethods
    with TestEnvironment
    with TestFiles
    with CoordinateSpaceTimeTests {
  lazy val reader = HadoopLayerReader[SpaceTimeKey, Tile, RasterMetaData](outputLocal)
  lazy val writer = HadoopLayerWriter(outputLocal)
  lazy val deleter = HadoopLayerDeleter(outputLocal)
  lazy val copier = HadoopLayerCopier[SpaceTimeKey, Tile, RasterMetaData](outputLocal)
  lazy val mover  = HadoopLayerMover[SpaceTimeKey, Tile, RasterMetaData](outputLocal)
  lazy val reindexer = HadoopLayerReindexer[SpaceTimeKey, Tile, RasterMetaData](outputLocal, ZCurveKeyIndexMethod.byPattern("YMM"))
  lazy val tiles = HadoopTileReader[SpaceTimeKey, Tile](outputLocal)
  lazy val sample =  CoordinateSpaceTime
}
