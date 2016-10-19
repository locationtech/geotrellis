package geotrellis.spark.io.cassandra

import geotrellis.raster.{Tile, TileFeature}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.testfiles.{TestFiles, TestTileFeatureFiles}

class CassandraTileFeatureSpatialSpec
  extends PersistenceSpec[SpatialKey, TileFeature[Tile, Tile], TileLayerMetadata[SpatialKey]]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with CassandraTestEnvironment
    with TestTileFeatureFiles
    with AllOnesTestTileFeatureSpec {

  lazy val instance       = BaseCassandraInstance(Seq("127.0.0.1"))
  lazy val attributeStore = CassandraAttributeStore(instance, "geotrellis_tf", "metadata")

  lazy val reader    = CassandraLayerReader(attributeStore)
  lazy val creader   = CassandraLayerCollectionReader(attributeStore)
  lazy val writer    = CassandraLayerWriter(attributeStore, "geotrellis_tf", "tiles")
  lazy val deleter   = CassandraLayerDeleter(attributeStore)
  lazy val updater   = CassandraLayerUpdater(attributeStore)
  lazy val tiles     = CassandraValueReader(attributeStore)
  lazy val sample    = AllOnesTestFile
  lazy val copier    = CassandraLayerCopier(attributeStore, reader, writer)
  lazy val reindexer = CassandraLayerReindexer(attributeStore, reader, writer, deleter, copier)
  lazy val mover     = CassandraLayerMover(copier, deleter)
}
