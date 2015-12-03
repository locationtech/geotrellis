package geotrellis.spark.io.s3

import com.github.nscala_time.time.Imports._
import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import org.joda.time.DateTime

abstract class S3SpaceTimeSpec
  extends PersistenceSpec[SpaceTimeKey, Tile]
          with OnlyIfCanRunSpark
          with TestEnvironment with TestFiles
          with CoordinateSpaceTimeTests
          with LayerUpdateSpaceTimeTileTests
          with LayerCopySpaceTimeTileTests[S3LayerHeader] {
  type Container = RasterRDD[SpaceTimeKey]
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
  lazy val reader = new S3LayerReader[SpaceTimeKey, Tile, RasterRDD[SpaceTimeKey]](attributeStore, rddReader, None)
  lazy val updater = new S3LayerUpdater[SpaceTimeKey, Tile, RasterRDD[SpaceTimeKey]](attributeStore, rddWriter, true)
  lazy val deleter = new S3LayerDeleter[SpaceTimeKey](attributeStore)
  lazy val tiles = new S3TileReader[SpaceTimeKey, Tile](attributeStore) {
    override val s3Client = new MockS3Client
  }
  lazy val sample =  CoordinateSpaceTime
}

class S3SpaceTimeZCurveByYearSpec extends S3SpaceTimeSpec {
  override val layerId = LayerId("sample-" + name, 1) // avoid test collisions
  lazy val writer = new S3LayerWriter[SpaceTimeKey, Tile, RasterRDD[SpaceTimeKey]](attributeStore, rddWriter, ZCurveKeyIndexMethod.byYear, bucket, prefix, true)
  lazy val copier = S3LayerCopier[SpaceTimeKey, Tile, RasterRDD](attributeStore, reader, writer)
}

class S3SpaceTimeZCurveByFuncSpec extends S3SpaceTimeSpec {
  override val layerId = LayerId("sample-" + name, 1) // avoid test collisions
  lazy val writer = new S3LayerWriter[SpaceTimeKey, Tile, RasterRDD[SpaceTimeKey]](attributeStore, rddWriter, ZCurveKeyIndexMethod.by{ x =>  if (x < DateTime.now) 1 else 0 }, bucket, prefix, true)
  lazy val copier = S3LayerCopier[SpaceTimeKey, Tile, RasterRDD](attributeStore, reader, writer)
}

class S3SpaceTimeHilbertSpec extends S3SpaceTimeSpec {
  override val layerId = LayerId("sample-" + name, 1) // avoid test collisions
  lazy val writer = new S3LayerWriter[SpaceTimeKey, Tile, RasterRDD[SpaceTimeKey]](attributeStore, rddWriter, HilbertKeyIndexMethod(DateTime.now - 20.years, DateTime.now, 4), bucket, prefix, true)
  lazy val copier = S3LayerCopier[SpaceTimeKey, Tile, RasterRDD](attributeStore, reader, writer)
}

class S3SpaceTimeHilbertWithResolutionSpec extends S3SpaceTimeSpec {
  override val layerId = LayerId("sample-" + name, 1) // avoid test collisions
  lazy val writer = new S3LayerWriter[SpaceTimeKey, Tile, RasterRDD[SpaceTimeKey]](attributeStore, rddWriter, HilbertKeyIndexMethod(2), bucket, prefix, true)
  lazy val copier = S3LayerCopier[SpaceTimeKey, Tile, RasterRDD](attributeStore, reader, writer)
}
