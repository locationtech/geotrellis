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

package geotrellis.spark.store.hadoop

import geotrellis.vector._
import geotrellis.vector.ProjectedExtent
import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.raster.testkit.RasterMatchers
import geotrellis.layers.hadoop.HdfsUtils
import geotrellis.spark._
import geotrellis.spark.store.hadoop.formats._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.testfiles._
import geotrellis.spark.tiling._

import org.apache.hadoop.fs.Path
import spire.syntax.cfor._
import org.scalatest._

import java.net.URI
import java.time.{LocalDateTime, ZoneId}


class HadoopGeoTiffRDDSpec
    extends FunSpec
    with Matchers
    with RasterMatchers
    with TestEnvironment
    with TestFiles {
  describe("HadoopGeoTiffRDD") {

    it("should filter by geometry") {
      val testGeoTiffPath = new Path(localFS.getWorkingDirectory, "spark/src/test/resources/all-ones.tif")
      val options = HadoopGeoTiffRDD.Options(partitionBytes=Some(1<<20), maxTileSize = Some(64))
      val geometry = Line(Point(141.7066667, -17.5200000), Point(142.1333333, -17.7))
      val fn = {( _: URI, key: ProjectedExtent) => key }
      val source1 =
        HadoopGeoTiffRDD
          .apply[ProjectedExtent, ProjectedExtent, Tile](testGeoTiffPath, fn, options, Some(geometry))
          .map(_._1)
      val source2 =
        HadoopGeoTiffRDD
          .apply[ProjectedExtent, ProjectedExtent, Tile](testGeoTiffPath, fn, options, None)
          .map(_._1)

      source1.collect.toSet.size should be < source2.collect.toSet.size
    }

    it("should read the same rasters when reading small windows or with no windows, Spatial, SinglebandGeoTiff") {
      val tilesDir = new Path(localFS.getWorkingDirectory, "raster/data/one-month-tiles/")
      val source1 = HadoopGeoTiffRDD.spatial(tilesDir)
      val source2 = HadoopGeoTiffRDD.spatial(tilesDir, HadoopGeoTiffRDD.Options(maxTileSize = Some(128)))

      val (_, md) = source1.collectMetadata[SpatialKey](FloatingLayoutScheme(256))

      val stitched1 = source1.tileToLayout(md).stitch
      val stitched2 = source2.tileToLayout(md).stitch

      assertEqual(stitched1, stitched2)
    }

    it("should read the same rasters when reading small windows or with no windows, Spatial, MultibandGeoTiff") {
      val path = "raster/data/one-month-tiles"
      val tilesDir = new Path(localFS.getWorkingDirectory, path)
      val source1 = HadoopGeoTiffRDD.spatialMultiband(tilesDir)
      val source2 = HadoopGeoTiffRDD.spatialMultiband(tilesDir, HadoopGeoTiffRDD.Options(maxTileSize = Some(128)))

      val (_, md) = source1.collectMetadata[SpatialKey](FloatingLayoutScheme(256))

      val stitched1 = source1.tileToLayout(md).stitch
      val stitched2 = source2.tileToLayout(md).stitch

      assertEqual(stitched1, stitched2)
    }

    it("should read the same rasters when reading small windows or with no windows, Temporal, SinglebandGeoTiff") {
      val path = "raster/data/one-month-tiles/"
      val tilesDir = new Path(localFS.getWorkingDirectory, path)

      val source1 = HadoopGeoTiffRDD.temporal(tilesDir, HadoopGeoTiffRDD.Options(
        timeTag = "ISO_TIME",
        timeFormat = "yyyy-MM-dd'T'HH:mm:ss"))

      val source2 = HadoopGeoTiffRDD.temporal(tilesDir, HadoopGeoTiffRDD.Options(
        timeTag = "ISO_TIME",
        timeFormat = "yyyy-MM-dd'T'HH:mm:ss",
        maxTileSize = Some(128)))

      val (wholeInfo, _) = source1.first()
      val dateTime = wholeInfo.time

      val collection = source2.map({ case (info, _) => info.time }).collect

      collection.forall({ t => t == dateTime }) should be (true)
    }

    it("should read the same rasters when reading small windows or with no windows, Temporal, MultibandGeoTiff") {
      val path = "raster/data/one-month-tiles-multiband"
      val tilesDir = new Path(localFS.getWorkingDirectory, path)

      val source1 = HadoopGeoTiffRDD.temporalMultiband(tilesDir, HadoopGeoTiffRDD.Options(
        timeTag = "ISO_TIME",
        timeFormat = "yyyy-MM-dd'T'HH:mm:ss"))

      val source2 = HadoopGeoTiffRDD.temporalMultiband(tilesDir, HadoopGeoTiffRDD.Options(
        timeTag = "ISO_TIME",
        timeFormat = "yyyy-MM-dd'T'HH:mm:ss",
        maxTileSize = Some(128)))

      val (wholeInfo, _) = source1.first()
      val dateTime = wholeInfo.time

      val collection = source2.map({ case (info, _) => info.time }).collect

      collection.forall({ t => t == dateTime }) should be (true)
    }

    it("should read the rasters with each raster path handling") {
      val tilesDir: Path = new Path(localFS.getWorkingDirectory, "raster/data/one-month-tiles/")
      val pattern = """-(\d+)*_""".r
      def zdtFromString(str: String) = {
        val n = pattern.findAllIn(str)
        n.next()
        val gr = n.group(1)
        LocalDateTime.of(gr.substring(0, 4).toInt, gr.substring(4, 6).toInt, 1, 0, 0, 0).atZone(ZoneId.of("UTC"))
      }

      val expected = HdfsUtils.listFiles(tilesDir, sc.hadoopConfiguration).map { path =>
        zdtFromString(path.getName).toInstant.toEpochMilli
      }.toSet

      val actual =
        HadoopGeoTiffRDD.singleband[ProjectedExtent, TemporalProjectedExtent](
          path = tilesDir,
          uriToKey = (u: URI, key: ProjectedExtent) => {
            val n = pattern.findAllIn(u.getPath.split("/").last)
            n.next()
            val gr = n.group(1)
            val zdt = LocalDateTime.of(gr.substring(0, 4).toInt, gr.substring(4, 6).toInt, 1, 0, 0, 0).atZone(ZoneId.of("UTC"))

            TemporalProjectedExtent(key, zdt)
          },
          options = HadoopGeoTiffRDD.Options.DEFAULT
        ).map(_._1.instant).collect().toSet


      actual should contain theSameElementsAs expected
    }
  }
}
