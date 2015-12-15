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
          with TestSparkContext
          with TestEnvironment with TestFiles
          with AllOnesTestTileTests {
  type Container = RasterRDD[SpatialKey]
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
  lazy val reader = new S3LayerReader[SpatialKey, Tile, RasterMetaData, Container](attributeStore, rddReader, None)
  lazy val updater = new S3LayerUpdater[SpatialKey, Tile, RasterMetaData, Container](attributeStore, rddWriter, true)
  lazy val deleter = new S3LayerDeleter(attributeStore) { override val getS3Client = () => new MockS3Client() }
  lazy val tiles = new S3TileReader[SpatialKey, Tile](attributeStore) {
    override val s3Client = new MockS3Client()
  }
  lazy val sample = AllOnesTestFile
}

class S3SpatialRowMajorSpec extends S3SpatialSpec {
  lazy val writer = new S3LayerWriter[SpatialKey, Tile, RasterMetaData, Container](attributeStore,rddWriter, RowMajorKeyIndexMethod, bucket, prefix, true)
}

class S3SpatialZCurveSpec extends S3SpatialSpec {
  lazy val writer = new S3LayerWriter[SpatialKey, Tile, RasterMetaData, Container](attributeStore,rddWriter, ZCurveKeyIndexMethod, bucket, prefix, true)
}

class S3SpatialHilbertSpec extends S3SpatialSpec {
  lazy val writer = new S3LayerWriter[SpatialKey, Tile, RasterMetaData, Container](attributeStore,rddWriter, HilbertKeyIndexMethod, bucket, prefix, true)
}
