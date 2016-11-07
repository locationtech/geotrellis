package geotrellis.spark.io.s3

import geotrellis.raster._
import geotrellis.raster.testkit.RasterMatchers
import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.s3.testkit._

import org.apache.hadoop.conf.Configuration
import com.amazonaws.auth.AWSCredentials
import org.apache.hadoop.mapreduce.{ TaskAttemptContext, InputSplit }

import java.nio.file.{ Paths, Files }
import org.scalatest._

class S3GeoTiffRDDSpec
  extends FunSpec
    with Matchers
    with RasterMatchers
    with TestEnvironment {

  describe("S3GeoTiffRDD") {
    val bucket = this.getClass.getSimpleName
    val key = "geoTiff/all-ones.tif"

    implicit val mockClient = new MockS3Client()
    val testGeoTiffPath = "spark/src/test/resources/all-ones.tif"
    val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))
    mockClient.putObject(bucket, key, geoTiffBytes)

    it("should read the same rasters when reading small windows or with no windows, Spatial, SinglebandGeoTiff") {
      val source1 = S3GeoTiffRDD.spatial(bucket, key, S3GeoTiffRDD.Options(getS3Client = () => new MockS3Client))
      val source2 = S3GeoTiffRDD.spatial(bucket, key, S3GeoTiffRDD.Options(maxTileSize = Some(128), getS3Client = () => new MockS3Client))

      source1.count should be < (source2.count)
      val (_, md) = source1.collectMetadata[SpatialKey](FloatingLayoutScheme(256))

      val stitched1 = source1.tileToLayout(md).stitch
      val stitched2 = source2.tileToLayout(md).stitch

      assertEqual(stitched1, stitched2)
    }
  }
}
