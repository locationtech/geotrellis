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

package geotrellis.spark.raster.io

import geotrellis.raster.io.geotiff._
import geotrellis.raster.testkit._
import geotrellis.spark._
import geotrellis.spark.testkit.TestEnvironment
import org.apache.hadoop.fs.Path
import org.scalatest._
import java.io._

import geotrellis.raster.{IntCellType, MultibandTile}

class HadoopWriterSpec extends FunSpec
  with Matchers
  with BeforeAndAfterAll
  with RasterMatchers
  with TileBuilders
  with TestEnvironment {

  describe ("writing Rasters without errors and with correct tiles, crs and extent using Hadoop output stream") {
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
    val existencePath = "raster-test/data/aspect.tif"

    it("should write GeoTiff with tags") {
      val geoTiff = MultibandGeoTiff(existencePath)

      val expected = geoTiff.tile
      val expectedTags = geoTiff.tags

      geoTiff.write(new Path(pathTiff))

      val actualTiff = GeoTiffHadoopReader.readMultiband(new Path(pathTiff))
      val actual = actualTiff.tile
      val actualTags = actualTiff.tags

      actual should be (expected)
      actualTags should be (expectedTags)
    }

    it("should write GeoTiff with tags with gzip") {
      val geoTiff = MultibandGeoTiff(existencePath)

      val expected = geoTiff.tile
      val expectedTags = geoTiff.tags

      geoTiff.write(new Path(pathTiff), true)

      val actualTiff = GeoTiffHadoopReader.readMultiband(new Path(s"$pathTiff.gz"))
      val actual = actualTiff.tile
      val actualTags = actualTiff.tags

      actual should be (expected)
      actualTags should be (expectedTags)
    }

    it("should write Png") {
      val geoTiff = expandGeoTiff(MultibandGeoTiff(existencePath))

      val expected = geoTiff.tile.convert(IntCellType).renderPng()
      expected.write(new Path(pathPng))

      val actual = PngHadoopReader.read(new Path(pathPng))

      actual.bytes should be (expected.bytes)
    }

    it("should write Png with gzip") {
      val geoTiff = expandGeoTiff(MultibandGeoTiff(existencePath))
      val expected = geoTiff.tile.convert(IntCellType).renderPng()
      expected.write(new Path(pathPng), true)

      val actual = PngHadoopReader.read(new Path(s"$pathPng.gz"))

      actual.bytes should be (expected.bytes)
    }

    it("should write Jpg") {
      val geoTiff = expandGeoTiff(MultibandGeoTiff(existencePath))
      val expected = geoTiff.tile.convert(IntCellType).renderJpg()
      expected.write(new Path(pathJpg))

      val actual = PngHadoopReader.read(new Path(pathJpg))

      actual.bytes should be (expected.bytes)
    }

    it("should write Jpg with gzip") {
      val geoTiff = expandGeoTiff(MultibandGeoTiff(existencePath))
      val expected = geoTiff.tile.convert(IntCellType).renderJpg()
      expected.write(new Path(pathJpg), true)

      val actual = PngHadoopReader.read(new Path(s"$pathJpg.gz"))

      actual.bytes should be (expected.bytes)
    }
  }
}
