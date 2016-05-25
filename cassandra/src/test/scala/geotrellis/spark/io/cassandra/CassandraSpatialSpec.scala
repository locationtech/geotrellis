package geotrellis.spark.io.cassandra

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.testfiles.TestFiles

class CassandraSpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, TileLayerMetadata[SpatialKey]]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with CassandraTestEnvironment
    with TestFiles
    with AllOnesTestTileSpec {

  lazy val instance       = BaseCassandraInstance(Seq("127.0.0.1"), "geotrellis")
  lazy val attributeStore = try {
    CassandraAttributeStore(instance)
  } catch {
    case e: Exception =>
      println("A script for setting up the Cassandra environment necessary to run these tests can be found at scripts/cassandraTestDB.sh - requires a working docker setup")
      throw e
  }

  lazy val reader    = CassandraLayerReader(attributeStore)
  lazy val writer    = CassandraLayerWriter(attributeStore, "tiles")
  lazy val deleter   = CassandraLayerDeleter(attributeStore)
  lazy val updater   = CassandraLayerUpdater(attributeStore)
  lazy val tiles     = CassandraValueReader(attributeStore)
  lazy val sample    = AllOnesTestFile
  lazy val copier    = CassandraLayerCopier(attributeStore, reader, writer)
  lazy val reindexer = CassandraLayerReindexer(attributeStore, reader, writer, deleter, copier)
  lazy val mover     = CassandraLayerMover(copier, deleter)
}
