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

package geotrellis.spark.io.s3

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.testkit.RasterMatchers
import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.s3.testkit._
import geotrellis.spark.testkit.TestEnvironment

import org.apache.hadoop.conf.Configuration
import com.amazonaws.auth.AWSCredentials
import org.apache.hadoop.mapreduce.{ TaskAttemptContext, InputSplit }

import java.nio.file.{ Paths, Files }
import spire.syntax.cfor._
import org.scalatest._

class S3GeoTiffRDDSpec
  extends FunSpec
    with Matchers
    with RasterMatchers
    with TestEnvironment {

  describe("S3GeoTiffRDD Spatial") {
    implicit val mockClient = new MockS3Client()
    val bucket = this.getClass.getSimpleName

    it("should read the same rasters when reading small windows or with no windows, Spatial, SinglebandGeoTiff") {

      val key = "geoTiff/all-ones.tif"
      val testGeoTiffPath = "spark/src/test/resources/all-ones.tif"
      val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))
      mockClient.putObject(bucket, key, geoTiffBytes)
      val source1 = S3GeoTiffRDD.spatial(bucket, key, S3GeoTiffRDD.Options(getS3Client = () => new MockS3Client))
      val source2 = S3GeoTiffRDD.spatial(bucket, key, S3GeoTiffRDD.Options(maxTileSize = Some(128), getS3Client = () => new MockS3Client))

      source1.count should be < (source2.count)
      val (_, md) = source1.collectMetadata[SpatialKey](FloatingLayoutScheme(256))

      val stitched1 = source1.tileToLayout(md).stitch
      val stitched2 = source2.tileToLayout(md).stitch

      assertEqual(stitched1, stitched2)
    }
    
    it("should read the same rasters when reading small windows or with no windows, Spatial, MultibandGeoTiff") {
      val key = "geoTiff/multi.tif"
      val testGeoTiffPath = "raster-test/data/geotiff-test-files/3bands/byte/3bands-striped-band.tif"
      val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))
      mockClient.putObject(bucket, key, geoTiffBytes)

      val source1 =
        S3GeoTiffRDD.spatialMultiband(bucket, key, S3GeoTiffRDD.Options(getS3Client = () => new MockS3Client))
      val source2 =
        S3GeoTiffRDD.spatialMultiband(bucket, key, S3GeoTiffRDD.Options(maxTileSize = Some(20), getS3Client = () => new MockS3Client))

      source1.count should be < (source2.count)
      val (_, md) = source1.collectMetadata[SpatialKey](FloatingLayoutScheme(20, 40))

      val stitched1 = source1.tileToLayout(md).stitch
      val stitched2 = source2.tileToLayout(md).stitch

      assertEqual(stitched1, stitched2)
    }
    
    it("should read the same rasters when reading small windows or with no windows, TemporalSpatial, SinglebandGeoTiff") {
      val key = "geoTiff/time.tif"
      val testGeoTiffPath = "raster-test/data/one-month-tiles/test-200506000000_0_0.tif"
      val geoTiffBytes = Files.readAllBytes(Paths.get(testGeoTiffPath))
      mockClient.putObject(bucket, key, geoTiffBytes)

      val source1 = S3GeoTiffRDD.temporal(bucket, key, S3GeoTiffRDD.Options(
        timeTag = "ISO_TIME",
        timeFormat = "yyyy-MM-dd'T'HH:mm:ss",
        getS3Client = () => new MockS3Client))

      val source2 = S3GeoTiffRDD.temporal(bucket, key, S3GeoTiffRDD.Options(
        maxTileSize = Some(128),
        timeTag = "ISO_TIME",
        timeFormat = "yyyy-MM-dd'T'HH:mm:ss",
        getS3Client = () => new MockS3Client))

      source1.count should be < (source2.count)

      val (wholeInfo, _) = source1.first
      val dateTime = wholeInfo.time

      val collection = source2.collect
      
      cfor(0)(_ < source2.count, _ + 1){ i =>
        val (info, _) = collection(i)

        info.time should be (dateTime)
      }
    }

    it("should read the same rasters when reading small windows or with no windows, TemporalSpatial, MultibandGeoTiff") {
      val key = "geoTiff/multi-time.tif"
      val path = "raster-test/data/one-month-tiles-multiband/result.tif"

      val singleband = GeoTiffReader.readSingleband(path)

      val multiTile = MultibandTile(singleband.tile, singleband.tile)
      val multiband = MultibandGeoTiff(multiTile, singleband.extent, singleband.crs, singleband.tags)

      val geoTiffBytes = multiband.toByteArray
      mockClient.putObject(bucket, key, geoTiffBytes)
      val source1 = S3GeoTiffRDD.temporalMultiband(bucket, key, S3GeoTiffRDD.Options(
        timeTag = "ISO_TIME",
        timeFormat = "yyyy-MM-dd'T'HH:mm:ss",
        getS3Client = () => new MockS3Client))

      val source2 = S3GeoTiffRDD.temporalMultiband(bucket, key, S3GeoTiffRDD.Options(
        maxTileSize = Some(256),
        timeTag = "ISO_TIME",
        timeFormat = "yyyy-MM-dd'T'HH:mm:ss",
        getS3Client = () => new MockS3Client))

      source1.count should be < (source2.count)

      val (wholeInfo, _) = source1.first()
      val dateTime = wholeInfo.time

      val collection = source2.collect
      
      cfor(0)(_ < source2.count, _ + 1){ i =>
        val (info, _) = collection(i)

        info.time should be (dateTime)
      }
    }
  }
}
