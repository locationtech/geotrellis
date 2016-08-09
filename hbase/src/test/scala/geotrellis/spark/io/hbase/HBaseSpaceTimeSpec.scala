package geotrellis.spark.io.hbase

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.testfiles.TestFiles

class HBaseSpaceTimeSpec
  extends PersistenceSpec[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]]
    with SpaceTimeKeyIndexMethods
    with HBaseTestEnvironment
    with TestFiles
    with CoordinateSpaceTimeSpec
    with LayerUpdateSpaceTimeTileSpec {

  registerAfterAll { () =>
    val instance = HBaseInstance(Seq("localhost"), "localhost")
    instance.getAdmin.disableTable("metadata")
    instance.getAdmin.disableTable("tiles")
    instance.getAdmin.deleteTable("metadata")
    instance.getAdmin.deleteTable("tiles")
    instance.getAdmin.close()
  }

  lazy val instance       = HBaseInstance(Seq("localhost"), "localhost")
  lazy val attributeStore = HBaseAttributeStore(instance)

  lazy val reader    = HBaseLayerReader(attributeStore)
  lazy val writer    = HBaseLayerWriter(attributeStore, "tiles")
  lazy val deleter   = HBaseLayerDeleter(attributeStore)
  lazy val updater   = HBaseLayerUpdater(attributeStore)
  lazy val tiles     = HBaseValueReader(attributeStore)
  lazy val sample    = CoordinateSpaceTime
  lazy val copier    = HBaseLayerCopier(attributeStore, reader, writer)
  lazy val reindexer = HBaseLayerReindexer(attributeStore, reader, writer, deleter, copier)
  lazy val mover     = HBaseLayerMover(copier, deleter)
}
