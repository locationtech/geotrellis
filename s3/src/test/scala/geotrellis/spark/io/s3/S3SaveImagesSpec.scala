package geotrellis.spark.render

import geotrellis.raster.{Tile, TileLayout}
import geotrellis.spark.{SpatialKey, LayerId}
import geotrellis.spark.TestEnvironment
import geotrellis.spark.render._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark.io.s3._
import geotrellis.spark.io.s3.SaveToS3

import org.scalatest._


class S3SaveImagesSpec extends FunSpec with TestEnvironment with Matchers {
  lazy val sample = TestFiles.generateSpatial("all-ones")
  val  mockClient = new MockS3Client()

  describe("Saving of Rendered Tiles to S3") {
    it ("should work with PNGs") {
      val template = "s3://mock-bucket/catalog/{name}/{z}/{x}/{y}.png"
      val id = LayerId("sample", 1)
      val bucket = "mock-bucket"
      val keyToPath = SaveToS3.spatialKeyToPath(id, template)
      val rdd = sample.renderPng().mapValues(_.bytes)
      val maker = { () => new MockS3Client() }

      SaveToS3(rdd, keyToPath, s3Maker = maker)
      rdd.collect().foreach { case (SpatialKey(col, row), bytes) =>
        mockClient.readBytes(bucket, s"catalog/sample/1/$col/$row.png") should be (bytes)
      }
    }

    it ("should work with JPEGs") {
      val template = "s3://mock-bucket/catalog/{name}/{z}/{x}/{y}.jpg"
      val id = LayerId("sample", 1)
      val bucket = "mock-bucket"
      val keyToPath = SaveToS3.spatialKeyToPath(id, template)
      val rdd = sample.renderPng().mapValues(_.bytes)
      val maker = { () => new MockS3Client() }

      SaveToS3(rdd, keyToPath, s3Maker = maker)
      rdd.collect().foreach { case (SpatialKey(col, row), bytes) =>
        mockClient.readBytes(bucket, s"catalog/sample/1/$col/$row.jpg") should be (bytes)
      }
    }

    it ("should work with GeoTIFFs") {
      val template = "s3://mock-bucket/catalog/{name}/{z}/{x}/{y}.tiff"
      val id = LayerId("sample", 1)
      val bucket = "mock-bucket"
      val keyToPath = SaveToS3.spatialKeyToPath(id, template)
      val rdd = sample.renderPng().mapValues(_.bytes)
      val maker = { () => new MockS3Client() }

      SaveToS3(rdd, keyToPath, s3Maker = maker)
      rdd.collect().foreach { case (SpatialKey(col, row), bytes) =>
        mockClient.readBytes(bucket, s"catalog/sample/1/$col/$row.tiff") should be (bytes)
      }
    }
  }
}
