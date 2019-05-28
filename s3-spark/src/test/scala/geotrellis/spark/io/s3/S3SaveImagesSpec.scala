/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.render

import geotrellis.raster.{Tile, TileLayout}
import geotrellis.tiling.SpatialKey
import geotrellis.layers._
import geotrellis.store.s3._
import geotrellis.spark.testkit.TestEnvironment
import geotrellis.spark.render._
import geotrellis.spark.testkit.testfiles.TestFiles
import geotrellis.spark.store.s3._
import geotrellis.spark.store.s3.testkit._
import geotrellis.spark.store.s3.SaveToS3

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model._

import org.scalatest._

class S3SaveImagesSpec extends FunSpec with TestEnvironment with Matchers {
  lazy val sample = TestFiles.generateSpatial("all-ones")
  val  mockClient = MockS3Client()
  S3TestUtils.cleanBucket(mockClient, "mock-bucket")
  S3ClientProducer.set(() => MockS3Client())

  describe("Saving of Rendered Tiles to S3") {
    it("should work with PNGs") {
      val template = "s3://mock-bucket/catalog/{name}/{z}/{x}/{y}.png"
      val id = LayerId("sample", 1)
      val bucket = "mock-bucket"
      val keyToPath = SaveToS3.spatialKeyToPath(id, template)
      val rdd = sample.renderPng().mapValues(_.bytes)
      val maker = { () => MockS3Client() }

      SaveToS3(rdd, keyToPath, s3Maker = maker)
      rdd.collect().foreach { case (SpatialKey(col, row), bytes) =>
        val getReq = GetObjectRequest.builder()
          .bucket(bucket)
          .key(s"catalog/sample/1/$col/$row.png")
          .build()
        mockClient.getObjectAsBytes(getReq).asByteArray should be (bytes)
      }
    }

    it("should work with JPEGs") {
      val template = "s3://mock-bucket/catalog/{name}/{z}/{x}/{y}.jpg"
      val id = LayerId("sample", 1)
      val bucket = "mock-bucket"
      val keyToPath = SaveToS3.spatialKeyToPath(id, template)
      val rdd = sample.renderPng().mapValues(_.bytes)
      val maker = { () => MockS3Client() }

      SaveToS3(rdd, keyToPath, s3Maker = maker)
      rdd.collect().foreach { case (SpatialKey(col, row), bytes) =>
        val getReq = GetObjectRequest.builder()
          .bucket(bucket)
          .key(s"catalog/sample/1/$col/$row.jpg")
          .build()
        mockClient.getObjectAsBytes(getReq).asByteArray should be (bytes)
      }
    }

    it("should work with GeoTIFFs") {
      val template = "s3://mock-bucket/catalog/{name}/{z}/{x}/{y}.tiff"
      val id = LayerId("sample", 1)
      val bucket = "mock-bucket"
      val keyToPath = SaveToS3.spatialKeyToPath(id, template)
      val rdd = sample.renderPng().mapValues(_.bytes)
      val maker = { () => MockS3Client() }

      SaveToS3(rdd, keyToPath, s3Maker = maker)
      rdd.collect().foreach { case (SpatialKey(col, row), bytes) =>
        val getReq = GetObjectRequest.builder()
          .bucket(bucket)
          .key(s"catalog/sample/1/$col/$row.tiff")
          .build()
        mockClient.getObjectAsBytes(getReq).asByteArray should be (bytes)
      }
    }
  }
}
