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

import geotrellis.raster.io.geotiff._
import geotrellis.raster.testkit._
import geotrellis.raster.{IntCellType, MultibandTile}
import geotrellis.layers._
import geotrellis.layers.hadoop._
import geotrellis.spark.store.hadoop
import geotrellis.spark.testkit.TestEnvironment

import org.apache.hadoop.fs.Path
import org.scalatest._

import java.io._

class HadoopRasterMethodsSpec extends FunSpec
  with Matchers
  with BeforeAndAfterAll
  with RasterMatchers
  with TileBuilders
  with TestEnvironment {

  describe ("writing Rasters without errors and with correct tiles, crs and extent using Hadoop FSData{Input|Output} stream") {
    def expandGeoTiff(geoTiff: MultibandGeoTiff) =
      MultibandGeoTiff(
        MultibandTile(
          geoTiff.tile.bands ++
            geoTiff.tile.bands ++
            geoTiff.tile.bands
        ),
        geoTiff.extent,
        geoTiff.crs
      )

    val (tempTiff, tempPng, tempJpg) = (
      File.createTempFile("geotiff-writer", ".tif"),
      File.createTempFile("geotiff-writer", ".png"),
      File.createTempFile("geotiff-writer", ".jpg")
    )

    val (pathTiff, pathPng, pathJpg) = (tempTiff.getPath, tempPng.getPath, tempJpg.getPath)
    val (pathTiffGz, pathPngGz, pathJpgGz) = (s"${tempTiff.getPath}.gz", s"${tempPng.getPath}.gz", s"${tempJpg.getPath}.gz")
    val existencePath = "raster/data/aspect.tif"

    it("should write GeoTiff with tags") {
      val geoTiff = MultibandGeoTiff(existencePath)

      val expected = geoTiff.tile.toArrayTile
      val expectedTags = geoTiff.tags

      geoTiff.write(new Path(pathTiff))

      val actualTiff = hadoop.HadoopGeoTiffReader.readMultiband(new Path(pathTiff))
      val actual = actualTiff.tile.toArrayTile
      val actualTags = actualTiff.tags

      actual should be (expected)
      actualTags should be (expectedTags)
    }

    it("should write GeoTiff with tags with gzip") {
      val geoTiff = MultibandGeoTiff(existencePath)

      val expected = geoTiff.tile.toArrayTile
      val expectedTags = geoTiff.tags

      geoTiff.write(new Path(pathTiffGz))

      val actualTiff = hadoop.HadoopGeoTiffReader.readMultiband(new Path(pathTiffGz))
      val actual = actualTiff.tile.toArrayTile
      val actualTags = actualTiff.tags

      actual should be (expected)
      actualTags should be (expectedTags)
    }

    it("should write Png") {
      val geoTiff = expandGeoTiff(MultibandGeoTiff(existencePath))

      val expected = geoTiff.tile.toArrayTile.convert(IntCellType).renderPng()
      expected.write(new Path(pathPng))

      val actual = hadoop.HadoopPngReader.read(new Path(pathPng))

      actual.bytes should be (expected.bytes)
    }

    it("should write Png with gzip") {
      val geoTiff = expandGeoTiff(MultibandGeoTiff(existencePath))
      val expected = geoTiff.tile.toArrayTile.convert(IntCellType).renderPng()
      expected.write(new Path(pathPngGz))

      val actual = hadoop.HadoopPngReader.read(new Path(pathPngGz))

      actual.bytes should be (expected.bytes)
    }

    it("should write Jpg") {
      val geoTiff = expandGeoTiff(MultibandGeoTiff(existencePath))
      val expected = geoTiff.tile.toArrayTile.convert(IntCellType).renderJpg()
      expected.write(new Path(pathJpg))

      val actual = hadoop.HadoopPngReader.read(new Path(pathJpg))

      actual.bytes should be (expected.bytes)
    }

    it("should write Jpg with gzip") {
      val geoTiff = expandGeoTiff(MultibandGeoTiff(existencePath))
      val expected = geoTiff.tile.toArrayTile.convert(IntCellType).renderJpg()
      expected.write(new Path(pathJpgGz))

      val actual = hadoop.HadoopPngReader.read(new Path(pathJpgGz))

      actual.bytes should be (expected.bytes)
    }
  }
}
