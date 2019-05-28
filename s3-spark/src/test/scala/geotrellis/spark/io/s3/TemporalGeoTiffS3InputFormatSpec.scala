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

import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.store.hadoop._
import geotrellis.spark.ingest._
import geotrellis.util.Filesystem
import geotrellis.spark.store.s3.testkit._
import geotrellis.spark.testkit.TestEnvironment

import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.core.sync.RequestBody
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.task._
import org.scalatest._

import java.time.ZoneOffset
import java.nio.file.{Files,Paths}
import java.time.format.DateTimeFormatter

class TemporalGeoTiffS3InputFormatSpec extends FunSpec with Matchers with TestEnvironment {
  val layoutScheme = ZoomedLayoutScheme(LatLng)
  val client = MockS3Client()
  val bucket = this.getClass.getSimpleName.toLowerCase
  S3TestUtils.cleanBucket(client, bucket)

  describe("SpaceTime GeoTiff S3 InputFormat") {
    it("should read the time from a file") {
      val path = new java.io.File(inputHomeLocalPath, "test-time-tag.tif").getPath

      val format = new TemporalGeoTiffS3InputFormat
      val conf = new Configuration(false)

      S3InputFormat.setCreateS3Client(conf, () => MockS3Client())
      TemporalGeoTiffS3InputFormat.setTimeTag(conf, "TIFFTAG_DATETIME")
      TemporalGeoTiffS3InputFormat.setTimeFormat(conf, "yyyy:MM:dd HH:mm:ss")

      val context = new TaskAttemptContextImpl(conf, new TaskAttemptID())
      val rr = format.createRecordReader(null, context)
      val (key, tile) = rr.read("key", Filesystem.slurp(path))

      DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss").withZone(ZoneOffset.UTC).format(key.time) should be ("2015:03:25 18:01:04")
    }

    val testGeoTiffPath = "spark/src/test/resources/nex-geotiff/tasmax_amon_BCSD_rcp26_r1i1p1_CONUS_CCSM4_200601-201012-200601120000_0_0.tif"
    val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))
    val putReq = PutObjectRequest.builder()
      .bucket(bucket)
      .key("nex-geotiff/tasmax.tiff")
      .build()
    val putBody = RequestBody.fromBytes(geoTiffBytes)
    client.putObject(putReq, putBody)

    it("should read GeoTiffs with ISO_TIME tag from S3") {
      val url = s"s3n://${bucket}/nex-geotiff/"
      val job = sc.newJob("temporal-geotiff-ingest")
      S3InputFormat.setCreateS3Client(job, () => MockS3Client())
      S3InputFormat.setUrl(job, url)
      S3InputFormat.setAnonymous(job)
      TemporalGeoTiffS3InputFormat.setTimeTag(job, "ISO_TIME")
      TemporalGeoTiffS3InputFormat.setTimeFormat(job, "yyyy-MM-dd'T'HH:mm:ss")

      val source = sc.newAPIHadoopRDD(job.getConfiguration,
        classOf[TemporalGeoTiffS3InputFormat],
        classOf[TemporalProjectedExtent],
        classOf[Tile])

      source.cache
      val sourceCount = source.count
      sourceCount should not be (0)
      info(s"Source RDD count: ${sourceCount}")

      Ingest[TemporalProjectedExtent, SpaceTimeKey](source, LatLng, layoutScheme){ (rdd, level) =>
        val rddCount = rdd.count
        rddCount should not be (0)
        info(s"Tiled RDD count: ${rddCount}")
      }
    }
  }
}
