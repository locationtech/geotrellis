package geotrellis.spark.io.s3

import com.github.nscala_time.time.Imports._
import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import org.joda.time.DateTime

abstract class S3SpaceTimeSpec
  extends PersistenceSpec[SpaceTimeKey, Tile, RasterMetaData]
          with TestSparkContext
          with TestEnvironment with TestFiles
          with CoordinateSpaceTimeTests
          with LayerUpdateSpaceTimeTileTests {

  val bucket = "mock-bucket"
  val prefix = "catalog"

  lazy val attributeStore = new S3AttributeStore(bucket, prefix) {
    override val s3Client = new MockS3Client
  }
  lazy val rddReader = new S3RDDReader[SpaceTimeKey, Tile]() {
    override val getS3Client = () => new MockS3Client
  }
  lazy val rddWriter = new S3RDDWriter[SpaceTimeKey, Tile](){
    override val getS3Client = () => new MockS3Client
  }

  lazy val reader = new S3LayerReader[SpaceTimeKey, Tile, RasterMetaData](attributeStore, rddReader, None)
  lazy val updater = new S3LayerUpdater[SpaceTimeKey, Tile, RasterMetaData](attributeStore, rddWriter, true)
  lazy val deleter = new S3LayerDeleter(attributeStore) { override val getS3Client = () => new MockS3Client }

  lazy val copier  = new S3LayerCopier[SpaceTimeKey, Tile, RasterMetaData](attributeStore, bucket, prefix) { override val getS3Client = () => new MockS3Client }
  lazy val reindexer = {
    val copier = new SparkLayerCopier[S3LayerHeader, SpaceTimeKey, Tile, RasterMetaData](
      attributeStore = attributeStore,
      layerReader    = reader,
      layerWriter    = new S3LayerWriter[SpaceTimeKey, Tile, RasterMetaData](
        attributeStore, rddWriter, ZCurveKeyIndexMethod.byPattern("YMM"), bucket, prefix, true
      )
    ) {
      def headerUpdate(id: LayerId, header: S3LayerHeader): S3LayerHeader =
        header.copy(bucket, key = makePath(prefix, s"${id.name}/${id.zoom}"))
    }

    val mover   = GenericLayerMover(copier, deleter)
    GenericLayerReindexer(deleter, copier, mover)
  }
  lazy val mover   = GenericLayerMover(copier, deleter)
  lazy val tiles   = new S3TileReader[SpaceTimeKey, Tile](attributeStore) {
    override val s3Client = new MockS3Client
  }
  lazy val sample =  CoordinateSpaceTime
}

class S3SpaceTimeZCurveByYearSpec extends S3SpaceTimeSpec {
  override val layerId = LayerId("sample-" + name, 1) // avoid test collisions
  lazy val writer = new S3LayerWriter[SpaceTimeKey, Tile, RasterMetaData](attributeStore, rddWriter, ZCurveKeyIndexMethod.byYear, bucket, prefix, true)
}

class S3SpaceTimeZCurveByFuncSpec extends S3SpaceTimeSpec {
  override val layerId = LayerId("sample-" + name, 1) // avoid test collisions
  lazy val writer = new S3LayerWriter[SpaceTimeKey, Tile, RasterMetaData](attributeStore, rddWriter, ZCurveKeyIndexMethod.by{ x =>  if (x < DateTime.now) 1 else 0 }, bucket, prefix, true)
}

class S3SpaceTimeHilbertSpec extends S3SpaceTimeSpec {
  override val layerId = LayerId("sample-" + name, 1) // avoid test collisions
  lazy val writer = new S3LayerWriter[SpaceTimeKey, Tile, RasterMetaData](attributeStore, rddWriter, HilbertKeyIndexMethod(DateTime.now - 20.years, DateTime.now, 4), bucket, prefix, true)
}

class S3SpaceTimeHilbertWithResolutionSpec extends S3SpaceTimeSpec {
  override val layerId = LayerId("sample-" + name, 1) // avoid test collisions
  lazy val writer = new S3LayerWriter[SpaceTimeKey, Tile, RasterMetaData](attributeStore, rddWriter, HilbertKeyIndexMethod(2), bucket, prefix, true)
}
