/*
 * Copyright 2019 Azavea
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

package geotrellis.raster.geotiff

import geotrellis.raster.{RasterSource, GridBounds}
import geotrellis.proj4.CRS
import geotrellis.raster.resample._
import geotrellis.raster.io.geotiff.GeoTiffTestUtils

import cats.instances.future._
import cats.instances.list._
import cats.syntax.traverse._

import org.scalatest._

import scala.concurrent.{ExecutionContext, Future}

class GeoTiffRasterSourceMultiThreadingSpec extends AsyncFunSpec with GeoTiffTestUtils {
  lazy val url = baseGeoTiffPath("vlm/aspect-tiled.tif")
  val source = GeoTiffRasterSource(url)

  implicit val ec = ExecutionContext.global

  val iterations = (0 to 30).toList

  /**
    * readBounds and readExtends are not covered by these tests since these methods return an [[Iterator]].
    * Due to the [[Iterator]] lazy nature, the lock in these cases should be done on the user side.
    * */
  def testMultithreading(rs: RasterSource): Unit = {
    it("read") {
      val res = iterations.map { _ => Future { rs.read() } }.sequence.map(_.flatten)
      res.map { rasters => rasters.length shouldBe iterations.length }
    }

    it("readBounds") {
      val bounds = GridBounds(rs.dimensions)
      val res =
        iterations
          .map { _ => Future { rs.read(bounds, 0 until rs.bandCount) } }
          .sequence
          .map(_.flatten)

      res.map { rasters => rasters.length shouldBe iterations.length }
    }
  }

  describe("GeoTiffRasterSource should be threadsafe") {
    testMultithreading(source)
  }

  describe("GeoTiffRasterReprojectSource should be threadsafe") {
    testMultithreading(source.reproject(CRS.fromEpsgCode(4326)))
  }

  describe("GeoTiffRasterResampleSource should be threadsafe") {
    testMultithreading(source.resample((source.cols * 0.95).toInt , (source.rows * 0.95).toInt, NearestNeighbor))
  }
}
