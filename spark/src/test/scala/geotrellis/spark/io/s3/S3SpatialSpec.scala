package geotrellis.spark.io.s3

import java.io.{ByteArrayOutputStream, ObjectOutputStream}

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._

class S3SpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, RasterMetaData]
    with SpatialKeyIndexMethods
    with TestEnvironment with TestFiles
    with AllOnesTestTileTests {

  val bucket = "mock-bucket"
  val prefix = "catalog"

  lazy val attributeStore = new S3AttributeStore(bucket, prefix) {
    override val s3Client = new MockS3Client()
  }
  lazy val rddReader = new S3RDDReader[SpatialKey, Tile]() {
    override  val getS3Client = () => new MockS3Client()
  }

  lazy val rddWriter = new S3RDDWriter {
    def getS3Client = () => {
      new MockS3Client()
    }
  }
  lazy val reader = new S3LayerReader[SpatialKey, Tile, RasterMetaData](attributeStore, rddReader, None)
  lazy val writer = new MockS3LayerWriter(attributeStore, bucket, prefix, S3LayerWriter.Options.DEFAULT)
  lazy val updater = new S3LayerUpdater[SpatialKey, Tile, RasterMetaData](attributeStore, rddWriter, true)
  lazy val deleter = new S3LayerDeleter(attributeStore) { override val getS3Client = () => new MockS3Client() }
  lazy val copier  = new S3LayerCopier[SpatialKey, Tile, RasterMetaData](attributeStore, bucket, prefix) { override val getS3Client = () => new MockS3Client }
  lazy val reindexer = {
    val copier = new SparkLayerCopier[S3LayerHeader, SpatialKey, Tile, RasterMetaData](
      attributeStore = attributeStore,
      layerReader    = reader,
      layerWriter    = writer
    ) {
      def headerUpdate(id: LayerId, header: S3LayerHeader): S3LayerHeader =
        header.copy(bucket, key = makePath(prefix, s"${id.name}/${id.zoom}"))
    }

    val mover = GenericLayerMover(copier, deleter)
    GenericLayerReindexer(deleter, copier, mover)
  }
  lazy val mover = GenericLayerMover(copier, deleter)
  lazy val tiles = new S3TileReader[SpatialKey, Tile](attributeStore) {
    override val s3Client = new MockS3Client()
  }
  lazy val sample = AllOnesTestFile
}
