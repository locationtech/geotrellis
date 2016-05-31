package geotrellis.spark.io.cassandra

import geotrellis.raster.{Tile, TileFeature}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.testfiles.TestTileFeatureFiles

class CassandraTileFeatureSpaceTimeSpec
  extends PersistenceSpec[SpaceTimeKey, TileFeature[Tile, Tile], TileLayerMetadata[SpaceTimeKey]]
    with SpaceTimeKeyIndexMethods
    with TestEnvironment
    with CassandraTestEnvironment
    with TestTileFeatureFiles
    with CoordinateSpaceTimeTileFeatureSpec
    with LayerUpdateSpaceTimeTileFeatureSpec {

  lazy val instance       = BaseCassandraInstance(Seq("127.0.0.1"))
  lazy val attributeStore = try {
    CassandraAttributeStore(instance, "geotrellis-tf", "metadata")
  } catch {
    case e: Exception =>
      println("A script for setting up the Cassandra environment necessary to run these tests can be found at scripts/cassandraTestDB.sh - requires a working docker setup")
      throw e
  }

  lazy val reader    = CassandraLayerReader(attributeStore)
  lazy val writer    = CassandraLayerWriter(attributeStore, "geotrellis-tf", "tiles")
  lazy val deleter   = CassandraLayerDeleter(attributeStore)
  lazy val updater   = CassandraLayerUpdater(attributeStore)
  lazy val tiles     = CassandraValueReader(attributeStore)
  lazy val sample    = CoordinateSpaceTime
  lazy val copier    = CassandraLayerCopier(attributeStore, reader, writer)
  lazy val reindexer = CassandraLayerReindexer(attributeStore, reader, writer, deleter, copier)
  lazy val mover     = CassandraLayerMover(copier, deleter)
}
