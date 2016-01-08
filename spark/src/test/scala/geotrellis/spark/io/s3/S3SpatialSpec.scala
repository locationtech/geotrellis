package geotrellis.spark.io.s3

import java.io.{ByteArrayOutputStream, ObjectOutputStream}

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.index.hilbert.HilbertSpatialKeyIndex
import geotrellis.spark.io.index.rowmajor.RowMajorSpatialKeyIndex
import geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._

abstract class S3SpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, RasterMetaData]
          with TestSparkContext
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

  lazy val deleter = new S3LayerDeleter(attributeStore) { override val getS3Client = () => new MockS3Client() }
  lazy val mover   = GenericLayerMover(copier, deleter)
  lazy val sample   = AllOnesTestFile
}

class S3SpatialRowMajorSpec extends S3SpatialSpec {
  lazy val reader = new S3LayerReader[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex](attributeStore, rddReader, None)
  lazy val updater = new S3LayerUpdater[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex](attributeStore, rddWriter, true)
  lazy val copier  = new S3LayerCopier[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex](attributeStore, bucket, prefix) { override val getS3Client = () => new MockS3Client }
  lazy val reindexer =
    S3LayerReindexer[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex, ZSpatialKeyIndex](
      attributeStore,
      ZCurveKeyIndexMethod,
      S3LayerReindexer.Options.DEFAULT.copy(getS3Client = () => new MockS3Client)
    )
  lazy val tiles = new S3TileReader[SpatialKey, Tile, RowMajorSpatialKeyIndex](attributeStore) {
    override val s3Client = new MockS3Client()
  }

  lazy val writer = new S3LayerWriter[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex](attributeStore,rddWriter, RowMajorKeyIndexMethod, bucket, prefix, true)
}

class S3SpatialZCurveSpec extends S3SpatialSpec {
  lazy val reader = new S3LayerReader[SpatialKey, Tile, RasterMetaData, ZSpatialKeyIndex](attributeStore, rddReader, None)
  lazy val updater = new S3LayerUpdater[SpatialKey, Tile, RasterMetaData, ZSpatialKeyIndex](attributeStore, rddWriter, true)
  lazy val copier  = new S3LayerCopier[SpatialKey, Tile, RasterMetaData, ZSpatialKeyIndex](attributeStore, bucket, prefix) { override val getS3Client = () => new MockS3Client }
  lazy val reindexer =
    S3LayerReindexer[SpatialKey, Tile, RasterMetaData, ZSpatialKeyIndex, ZSpatialKeyIndex](
      attributeStore,
      ZCurveKeyIndexMethod,
      S3LayerReindexer.Options.DEFAULT.copy(getS3Client = () => new MockS3Client)
    )
  lazy val tiles = new S3TileReader[SpatialKey, Tile, ZSpatialKeyIndex](attributeStore) {
    override val s3Client = new MockS3Client()
  }

  lazy val writer = new S3LayerWriter[SpatialKey, Tile, RasterMetaData, ZSpatialKeyIndex](attributeStore,rddWriter, ZCurveKeyIndexMethod, bucket, prefix, true)
}

class S3SpatialHilbertSpec extends S3SpatialSpec {
  lazy val reader = new S3LayerReader[SpatialKey, Tile, RasterMetaData, HilbertSpatialKeyIndex](attributeStore, rddReader, None)
  lazy val updater = new S3LayerUpdater[SpatialKey, Tile, RasterMetaData, HilbertSpatialKeyIndex](attributeStore, rddWriter, true)
  lazy val copier  = new S3LayerCopier[SpatialKey, Tile, RasterMetaData, HilbertSpatialKeyIndex](attributeStore, bucket, prefix) { override val getS3Client = () => new MockS3Client }
  lazy val reindexer =
    S3LayerReindexer[SpatialKey, Tile, RasterMetaData, HilbertSpatialKeyIndex, ZSpatialKeyIndex](
      attributeStore,
      ZCurveKeyIndexMethod,
      S3LayerReindexer.Options.DEFAULT.copy(getS3Client = () => new MockS3Client)
    )
  lazy val tiles = new S3TileReader[SpatialKey, Tile, HilbertSpatialKeyIndex](attributeStore) {
    override val s3Client = new MockS3Client()
  }

  lazy val writer = new S3LayerWriter[SpatialKey, Tile, RasterMetaData, HilbertSpatialKeyIndex](attributeStore,rddWriter, HilbertKeyIndexMethod, bucket, prefix, true)
}
