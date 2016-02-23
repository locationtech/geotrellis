package geotrellis.spark.render

import geotrellis.raster.{Tile, TileLayout}
import geotrellis.spark.{SpatialKey, LayerId}
import geotrellis.spark.TestEnvironment
import geotrellis.spark.render._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark.io.s3._
import geotrellis.spark.io.s3.SaveToS3Methods
import geotrellis.spark.io.hadoop._

import org.scalatest._


class RenderedImageSpec extends FunSpec with TestEnvironment {
  lazy val sample = TestFiles.generateSpatial("all-ones", TestFiles.rasterMetaData)

  describe("Saving of Rendered Tiles to S3") {
    it ("should work with PNGs") {
      val template = "s3://mock-bucket/catalog/{name}/{z}/{x}/{y}.png"
      val id = LayerId("sample", 1)
      val bucket = "mock-bucket"
      val keyToPath = SaveToS3Methods.spatialKeyToPath(id, template)
      val rdd = sample.renderPng().rdd
      val maker = { () => new MockS3Client() }

      SaveToS3Methods(bucket, keyToPath, rdd, maker)
    }

    it ("should work with JPEGs") {
      val template = "s3://mock-bucket/catalog/{name}/{z}/{x}/{y}.jpg"
      val id = LayerId("sample", 1)
      val bucket = "mock-bucket"
      val keyToPath = SaveToS3Methods.spatialKeyToPath(id, template)
      val rdd = sample.renderPng().rdd
      val maker = { () => new MockS3Client() }

      SaveToS3Methods(bucket, keyToPath, rdd, maker)
    }

    it ("should work with GeoTIFFs") {
      val template = "s3://mock-bucket/catalog/{name}/{z}/{x}/{y}.tiff"
      val id = LayerId("sample", 1)
      val bucket = "mock-bucket"
      val keyToPath = SaveToS3Methods.spatialKeyToPath(id, template)
      val rdd = sample.renderPng().rdd
      val maker = { () => new MockS3Client() }

      SaveToS3Methods(bucket, keyToPath, rdd, maker)
    }
  }

  describe("Saving of Rendered Tiles to Hadoop") {
    it ("should work with PNGs") {
      val template = "file:/tmp/testFiles/catalog/{name}/{z}/{x}/{y}.png"
      val id = LayerId("sample", 1)
      val keyToPath = SaveToHadoopMethods.spatialKeyToPath(id, template)
      sample.renderPng().saveToHadoop(keyToPath)
    }

    it ("should work with JPGs") {
      val template = "file:/tmp/testFiles/catalog/{name}/{z}/{x}/{y}.jpg"
      val id = LayerId("sample", 1)
      val keyToPath = SaveToHadoopMethods.spatialKeyToPath(id, template)
      sample.renderJpg().saveToHadoop(keyToPath)
    }

    it ("should work with GeoTiffs") {
      val template = "file:/tmp/testFiles/catalog/{name}/{z}/{x}/{y}.tiff"
      val id = LayerId("sample", 1)
      val keyToPath = SaveToHadoopMethods.spatialKeyToPath(id, template)
      sample.renderGeoTiff().saveToHadoop(keyToPath)
    }
  }
}
