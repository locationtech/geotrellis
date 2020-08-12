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

package geotrellis.spark.store.s3

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.spark.store.hadoop._
import geotrellis.spark.testkit.TestEnvironment

import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.core.sync.RequestBody

import java.nio.file.{Paths, Files}

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class GeoTiffS3InputFormatSpec extends AnyFunSpec with TestEnvironment with Matchers {

  val mockClient = MockS3Client()
  val bucket = this.getClass.getSimpleName.toLowerCase
  S3TestUtils.cleanBucket(mockClient, bucket)

  val testGeoTiffPath = "spark/src/test/resources/all-ones.tif"
  val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))
  val putReq = PutObjectRequest.builder()
    .bucket(bucket)
    .key("geotiff/all-ones.tif")
    .build()
  val putBody = RequestBody.fromBytes(geoTiffBytes)
  mockClient.putObject(putReq, putBody)

  describe("GeoTiff S3 InputFormat") {
    val url = s"s3n://${bucket}/geotiff"

    it("should read GeoTiffs from S3") {
      val job = sc.newJob("geotiff-ingest")
      S3InputFormat.setUrl(job, url)

      S3InputFormat.setCreateS3Client(job, { () => MockS3Client() })

      val source = sc.newAPIHadoopRDD(job.getConfiguration,
        classOf[GeoTiffS3InputFormat],
        classOf[ProjectedExtent],
        classOf[Tile])
      source.map(x => x).cache
      val sourceCount = source.count
      sourceCount should not be (0)
      info(s"Source RDD count: ${sourceCount}")
    }

    it("should set the CRS") {
      val job = sc.newJob("geotiff-ingest")
      S3InputFormat.setUrl(job, url)
      S3InputFormat.setCreateS3Client(job, { () => MockS3Client() })
      GeoTiffS3InputFormat.setCrs(job, WebMercator)

      val source = sc.newAPIHadoopRDD(job.getConfiguration,
        classOf[GeoTiffS3InputFormat],
        classOf[ProjectedExtent],
        classOf[Tile])
      source.map(x=>x).cache
      val sourceCount = source.count
      sourceCount should not be (0)
      info(s"Source RDD count: ${sourceCount}")
    }
  }
}
