package geotrellis.spark.io.s3

import java.io.{ByteArrayOutputStream, ObjectOutputStream}

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._

abstract class S3SpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, RasterMetaData]
    with TestEnvironment with TestFiles
    with AllOnesTestTileTests {

  val bucket = "mock-bucket"
  val prefix = "catalog"
  override val layerId = LayerId("sample-" + name, 1) // avoid test collisions

  lazy val attributeStore = new S3AttributeStore(bucket, prefix) {
    override val s3Client = new MockS3Client()
  }
  lazy val rddReader = new S3RDDReader[SpatialKey, Tile]() {
    override  val getS3Client = () => new MockS3Client()
  }
  lazy val rddWriter = new S3RDDWriter[SpatialKey, Tile](){
    override val getS3Client = () => {
      val client = new MockS3Client()

      client
    }
  }
  lazy val reader = new S3LayerReader[SpatialKey, Tile, RasterMetaData](attributeStore, rddReader, None)
  lazy val updater = new S3LayerUpdater[SpatialKey, Tile, RasterMetaData](attributeStore, rddWriter, true)
  lazy val deleter = new S3LayerDeleter(attributeStore) { override val getS3Client = () => new MockS3Client() }
  lazy val copier  = new S3LayerCopier[SpatialKey, Tile, RasterMetaData](attributeStore, bucket, prefix) { override val getS3Client = () => new MockS3Client }
  lazy val reindexer = {
    val copier = new SparkLayerCopier[S3LayerHeader, SpatialKey, Tile, RasterMetaData](
      attributeStore = attributeStore,
      layerReader    = reader,
      layerWriter    = new S3LayerWriter[SpatialKey, Tile, RasterMetaData](
        attributeStore, rddWriter, ZCurveKeyIndexMethod, bucket, prefix, true
      )
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

class S3SpatialRowMajorSpec extends S3SpatialSpec {
  lazy val writer = new S3LayerWriter[SpatialKey, Tile, RasterMetaData](attributeStore,rddWriter, RowMajorKeyIndexMethod, bucket, prefix, true)
}

class S3SpatialZCurveSpec extends S3SpatialSpec {
  lazy val writer = new S3LayerWriter[SpatialKey, Tile, RasterMetaData](attributeStore,rddWriter, ZCurveKeyIndexMethod, bucket, prefix, true)
}

class S3SpatialHilbertSpec extends S3SpatialSpec {
  lazy val writer = new S3LayerWriter[SpatialKey, Tile, RasterMetaData](attributeStore,rddWriter, HilbertKeyIndexMethod, bucket, prefix, true)
}
