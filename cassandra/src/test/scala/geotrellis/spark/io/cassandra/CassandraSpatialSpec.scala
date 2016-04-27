package geotrellis.spark.io.cassandra

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.testfiles.TestFiles

class CassandraSpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, TileLayerMetadata[SpatialKey]]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with TestFiles
    with AllOnesTestTileTests {

  override def beforeAllPesisteceSpec() = {
    CassandraMock.start
  }

  lazy val instance = BaseCassandraInstance(Seq("localhost"), "geotrellis")

  lazy val reader    = CassandraLayerReader(instance)
  lazy val writer    = CassandraLayerWriter(instance, "tiles")
  lazy val deleter   = CassandraLayerDeleter(instance)
  lazy val reindexer = CassandraLayerReindexer(instance)
  lazy val updater   = CassandraLayerUpdater(instance)
  lazy val tiles     = CassandraValueReader(instance)
  lazy val sample    = AllOnesTestFile

  lazy val copier = CassandraLayerCopier(instance, reader, writer)
  lazy val mover  = CassandraLayerMover(copier, deleter)
}
