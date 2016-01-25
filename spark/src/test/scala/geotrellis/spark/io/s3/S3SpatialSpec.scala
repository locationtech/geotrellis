package geotrellis.spark.io.s3

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._

abstract class S3SpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, RasterMetadata]
    with TestEnvironment with TestFiles
    with AllOnesTestTileTests {

  val bucket = "mock-bucket"
  val prefix = "catalog"

  lazy val reindexerKeyIndexMethod: KeyIndexMethod[SpatialKey] = ZCurveKeyIndexMethod
  
  override val layerId = LayerId("sample-" + name, 1) // avoid test collisions

  lazy val attributeStore = new S3AttributeStore(bucket, prefix) {
    override val s3Client = new MockS3Client()
  }
  lazy val rddReader = new S3RDDReader[SpatialKey, Tile]() {
    override  val getS3Client = () => new MockS3Client()
  }
  lazy val rddWriter = new S3RDDWriter[SpatialKey, Tile](){
    override val getS3Client = () => new MockS3Client()
  }

  lazy val reader    = new S3LayerReader[SpatialKey, Tile, RasterMetadata](attributeStore, rddReader, None)
  lazy val updater   = new S3LayerUpdater[SpatialKey, Tile, RasterMetadata](attributeStore, rddWriter, true)
  lazy val deleter   = new S3LayerDeleter(attributeStore) { override val getS3Client = () => new MockS3Client() }
  lazy val copier    = new S3LayerCopier[SpatialKey, Tile, RasterMetadata](attributeStore, bucket, prefix) { override val getS3Client = () => new MockS3Client }
  lazy val reindexer = S3LayerReindexer[SpatialKey, Tile, RasterMetadata](attributeStore, S3LayerReindexer.Options.DEFAULT.copy(getS3Client = () => new MockS3Client))
  lazy val tiles     = new S3TileReader[SpatialKey, Tile](attributeStore) {
    override val s3Client = new MockS3Client()
  }
  lazy val mover  = GenericLayerMover(copier, deleter)
  lazy val writer = new S3LayerWriter[SpatialKey, Tile, RasterMetadata](attributeStore, rddWriter, bucket, prefix, true)
  lazy val sample = AllOnesTestFile
}

class S3SpatialRowMajorSpec extends S3SpatialSpec {
  lazy val writerKeyIndexMethod: KeyIndexMethod[SpatialKey] = RowMajorKeyIndexMethod
}

class S3SpatialZCurveSpec extends S3SpatialSpec {
  lazy val writerKeyIndexMethod: KeyIndexMethod[SpatialKey] = ZCurveKeyIndexMethod
}

class S3SpatialHilbertSpec extends S3SpatialSpec {
  lazy val writerKeyIndexMethod: KeyIndexMethod[SpatialKey] = HilbertKeyIndexMethod
}
