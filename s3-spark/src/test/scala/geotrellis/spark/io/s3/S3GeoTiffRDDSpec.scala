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

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.testkit.RasterMatchers
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.store.s3.testkit._
import geotrellis.spark.testkit.TestEnvironment
import geotrellis.vector._

import spire.syntax.cfor._
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model._

import java.nio.file.{Files, Paths}

import org.scalatest._

import org.apache.spark.rdd.RDD


class S3GeoTiffRDDSpec
  extends FunSpec
    with Matchers
    with RasterMatchers
    with TestEnvironment
    with BeforeAndAfterEach {

  override def afterEach() {
    super.afterEach()
  }

  implicit def toOption[T](t: T): Option[T] = Option(t)

  describe("S3GeoTiffRDD") {
    val mockClient = MockS3Client()
    val bucket = this.getClass.getSimpleName.toLowerCase
    S3TestUtils.cleanBucket(mockClient, bucket)

    it("should filter by geometry") {
      val key = "geoTiff/all-ones.tif"
      val testGeoTiffPath = "spark/src/test/resources/all-ones.tif"
      val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))
      val putRequest = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build()
      val putBody = RequestBody.fromBytes(geoTiffBytes)
      mockClient.putObject(putRequest, putBody)

      val options = S3GeoTiffRDD.Options(getClient = () => MockS3Client(), partitionBytes=1<<20, maxTileSize = Some(64))
      val geometry = Line(Point(141.7066667, -17.5200000), Point(142.1333333, -17.7))
      val fn = {( _: Any, key: ProjectedExtent) => key }
      val source1 =
        S3GeoTiffRDD
          .apply[ProjectedExtent, ProjectedExtent, Tile](bucket, key, fn, options, Some(geometry))
          .map(_._1)
      val source2 =
        S3GeoTiffRDD
          .apply[ProjectedExtent, ProjectedExtent, Tile](bucket, key, fn, options, None)
          .map(_._1)

      source1.collect.toSet.size should be < source2.collect.toSet.size
    }

    it("should read the same rasters when reading small windows or with no windows, Spatial, SinglebandGeoTiff") {
      val key = "geoTiff/all-ones.tif"
      val testGeoTiffPath = "spark/src/test/resources/all-ones.tif"
      val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))
      val putReq = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build()
      val putBody = RequestBody.fromBytes(geoTiffBytes)
      mockClient.putObject(putReq, putBody)

      val source1 =
        S3GeoTiffRDD.spatial(bucket, key, S3GeoTiffRDD.Options(maxTileSize = None, partitionBytes = None, getClient = () => MockS3Client()))
      val source2 =
        S3GeoTiffRDD.spatial(bucket, key, S3GeoTiffRDD.Options(maxTileSize = Some(128), getClient = () => MockS3Client()))

      source1.count should be < (source2.count)

      val (_, md) = source1.collectMetadata[SpatialKey](FloatingLayoutScheme(256))

      val stitched1: Tile = source1.tileToLayout(md).stitch.tile
      val stitched2: Tile = source2.tileToLayout(md).stitch.tile

      assertEqual(stitched1, stitched2)
    }

    it("should read the same rasters when reading small windows or with no windows, Spatial, MultibandGeoTiff") {
      val key = "geoTiff/multi.tif"
      val testGeoTiffPath = "raster/data/geotiff-test-files/3bands/byte/3bands-striped-band.tif"
      val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))
      val putReq = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build()
      val putBody = RequestBody.fromBytes(geoTiffBytes)
      mockClient.putObject(putReq, putBody)

      val source1 =
        S3GeoTiffRDD.spatialMultiband(bucket, key, S3GeoTiffRDD.Options(maxTileSize=None, partitionBytes = None, getClient = () => MockS3Client()))
      val source2 = {
        S3GeoTiffRDD.spatialMultiband(bucket, key, S3GeoTiffRDD.Options(maxTileSize=Some(128), getClient = () => MockS3Client()))
      }

      //source1.count should be < (source2.count)
      val (_, md) = source1.collectMetadata[SpatialKey](FloatingLayoutScheme(20, 40))

      val stitched1 = source1.tileToLayout(md).stitch
      val stitched2 = source2.tileToLayout(md).stitch

      assertEqual(stitched1, stitched2)
    }

    it("should read the same rasters when reading small windows or with no windows, TemporalSpatial, SinglebandGeoTiff") {
      val key = "geoTiff/time.tif"
      val testGeoTiffPath = "raster/data/one-month-tiles/test-200506000000_0_0.tif"
      val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))
      val putReq = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build()
      val putBody = RequestBody.fromBytes(geoTiffBytes)
      mockClient.putObject(putReq, putBody)

      val source1 = S3GeoTiffRDD.temporal(bucket, key, S3GeoTiffRDD.Options(
        maxTileSize=None,
        partitionBytes = None,
        timeTag = "ISO_TIME",
        timeFormat = "yyyy-MM-dd'T'HH:mm:ss",
        getClient = () => MockS3Client()))

      val source2 = {
        S3GeoTiffRDD.temporal(bucket, key, S3GeoTiffRDD.Options(
          maxTileSize=Some(128),
          timeTag = "ISO_TIME",
          timeFormat = "yyyy-MM-dd'T'HH:mm:ss",
          getClient = () => MockS3Client()))
      }

      source1.count should be < (source2.count)

      val (wholeInfo, _) = source1.first
      val dateTime = wholeInfo.time

      val collection = source2.collect
      val length = source2.count

      cfor(0)(_ < length, _ + 1) { i =>
        val (info, _) = collection(i)

        info.time should be(dateTime)
      }
    }

    it("should read the same rasters when reading small windows or with no windows, TemporalSpatial, MultibandGeoTiff") {
      val key = "geoTiff/multi-time.tif"
      val path = "raster/data/one-month-tiles-multiband/result.tif"

      val singleband = GeoTiffReader.readSingleband(path)

      val multiTile = MultibandTile(singleband.tile, singleband.tile)
      val multiband = MultibandGeoTiff(multiTile, singleband.extent, singleband.crs, singleband.tags)

      val geoTiffBytes = multiband.toByteArray
      val putReq = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build()
      val putBody = RequestBody.fromBytes(geoTiffBytes)
      mockClient.putObject(putReq, putBody)
      val source1 = S3GeoTiffRDD.temporalMultiband(bucket, key, S3GeoTiffRDD.Options(
        maxTileSize = None,
        partitionBytes = None,
        timeTag = "ISO_TIME",
        timeFormat = "yyyy-MM-dd'T'HH:mm:ss",
        getClient = () => MockS3Client()))

      val source2 = {
        S3GeoTiffRDD.temporalMultiband(bucket, key, S3GeoTiffRDD.Options(
          maxTileSize = Some(256),

          timeTag = "ISO_TIME",
          timeFormat = "yyyy-MM-dd'T'HH:mm:ss",
          getClient = () => MockS3Client()))
      }

      source1.count should be < (source2.count)

      val (wholeInfo, _) = source1.first()
      val dateTime = wholeInfo.time

      val collection = source2.collect

      cfor(0)(_ < source2.count, _ + 1){ i =>
        val (info, _) = collection(i)

        info.time should be (dateTime)
      }
    }

    it("should read with num partitions and window size options set") {
      val key = "geoTiff/all-ones.tif"
      val testGeoTiffPath = "spark/src/test/resources/all-ones.tif"
      val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))
      val putReq = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build()
      val putBody = RequestBody.fromBytes(geoTiffBytes)
      mockClient.putObject(putReq, putBody)

      val source =
        S3GeoTiffRDD.spatial(bucket, key, S3GeoTiffRDD.Options(maxTileSize = 512, numPartitions = 32, getClient = () => MockS3Client()))

      source.count.toInt should be > 0
    }
  }
}
