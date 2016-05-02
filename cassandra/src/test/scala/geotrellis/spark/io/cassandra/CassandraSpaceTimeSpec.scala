package geotrellis.spark.io.cassandra

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.testfiles.TestFiles

class CassandraSpaceTimeSpec
  extends PersistenceSpec[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]]
    with SpaceTimeKeyIndexMethods
    with TestEnvironment
    with TestFiles
    /*with CoordinateSpaceTimeTests
    with LayerUpdateSpaceTimeTileTests*/ {

  lazy val instance = BaseCassandraInstance(Seq("127.0.0.1"), "geotrellis")

  lazy val reader    = CassandraLayerReader(instance)
  lazy val writer    = CassandraLayerWriter(instance, "tiles")
  lazy val deleter   = CassandraLayerDeleter(instance)
  lazy val reindexer = CassandraLayerReindexer(instance)
  lazy val updater   = CassandraLayerUpdater(instance)
  lazy val tiles     = CassandraValueReader(instance)
  lazy val sample    = CoordinateSpaceTime
  lazy val copier    = CassandraLayerCopier(instance, reader, writer)
  lazy val mover     = CassandraLayerMover(copier, deleter)
}
