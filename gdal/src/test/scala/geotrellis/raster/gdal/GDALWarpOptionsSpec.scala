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

package geotrellis.raster.gdal

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.OverviewStrategy
import geotrellis.raster.resample._
import geotrellis.vector.Extent
import geotrellis.raster.testkit._

import cats.syntax.option._
import org.gdal.gdal._

import scala.collection.JavaConverters._

import org.scalatest.GivenWhenThen
import org.scalatest.funspec.AnyFunSpec

class GDALWarpOptionsSpec extends AnyFunSpec with RasterMatchers with GivenWhenThen {
  import GDALWarpOptionsSpec._

  org.gdal.gdal.gdal.AllRegister()

  val filePath = Resource.path("vlm/aspect-tiled.tif")
  def filePathByIndex(i: Int): String = Resource.path(s"vlm/aspect-tiled-$i.tif")

  val reprojectOptions: GDALWarpOptions =
    generateWarpOptions(
      sourceCRS = lccProjection.some,
      targetCRS = WebMercator.some,
      cellSize  = CellSize(10, 10).some,
      te        = Extent(-8769160.0, 4257700.0, -8750640.0, 4274460.0).some
    )

  val resampleOptions: GDALWarpOptions =
    generateWarpOptions(
      et        = None,
      cellSize  = CellSize(22, 22).some,
      sourceCRS = lccProjection.some,
      targetCRS = WebMercator.some,
      te        = Extent(-8769178.0, 4257682.0, -8750640.0, 4274468.0).some
    )

  def rasterSourceFromUriOptions(uri: String, options: GDALWarpOptions): GDALRasterSource = GDALRasterSource(uri, options)

  def dsreprojectOpt(uri: String): GDALRasterSource = {
    val opts =
      GDALWarpOptions
        .EMPTY
        .reproject(
          rasterExtent = GridExtent(Extent(630000.0, 215000.0, 645000.0, 228500.0), 10, 10),
          lccProjection,
          WebMercator,
          TargetCellSize(CellSize(10, 10))
        )
    rasterSourceFromUriOptions(uri, opts)
  }

  def dsresampleOpt(uri: String): GDALRasterSource = {
    val opts =
      GDALWarpOptions
        .EMPTY
        .reproject(
          rasterExtent = GridExtent(Extent(630000.0, 215000.0, 645000.0, 228500.0), 10, 10),
          lccProjection,
          WebMercator,
          TargetCellSize(CellSize(10, 10))
        )
        .resample(
          GridExtent(Extent(-8769160.0, 4257700.0, -8750640.0, 4274460.0), CellSize(10, 10)),
          TargetRegion(GridExtent[Long](Extent(-8769160.0, 4257700.0, -8750640.0, 4274460.0), CellSize(22, 22)))
        )
    rasterSourceFromUriOptions(uri, opts)
  }

  describe("GDALWarp transformations") {
    def datasetToRasterExtent(ds: Dataset): RasterExtent = {
      val transform = ds.GetGeoTransform
      val width = ds.GetRasterXSize
      val height = ds.GetRasterYSize
      val x1 = transform(0)
      val y1 = transform(3)
      val x2 = x1 + transform(1) * width
      val y2 = y1 + transform(5) * height
      val e = Extent(
        math.min(x1, x2),
        math.min(y1, y2),
        math.max(x1, x2),
        math.max(y1, y2)
      )

      RasterExtent(e, math.abs(transform(1)), math.abs(transform(5)), width, height)
    }

    it("optimized transformation should behave in a same way as a list of warp applications") {
      val base = filePath

      val optimizedReproject = dsreprojectOpt(base)
      val optimizedResample = dsresampleOpt(base)

      val reprojectWarpAppOptions = new WarpOptions(new java.util.Vector(reprojectOptions.toWarpOptionsList.asJava))
      val resampleWarpAppOptions = new WarpOptions(new java.util.Vector(resampleOptions.toWarpOptionsList.asJava))
      val underlying = org.gdal.gdal.gdal.Open(filePath, org.gdal.gdalconst.gdalconstConstants.GA_ReadOnly)
      val originalReproject = org.gdal.gdal.gdal.Warp("/dev/null", Array(underlying), reprojectWarpAppOptions)
      val originalResample = org.gdal.gdal.gdal.Warp("/dev/null", Array(originalReproject), resampleWarpAppOptions)

      datasetToRasterExtent(originalReproject) shouldBe optimizedReproject.gridExtent.toRasterExtent
      datasetToRasterExtent(originalResample) shouldBe optimizedResample.gridExtent.toRasterExtent

      // cleanup JNI objects
      originalResample.delete()
      originalReproject.delete()
      underlying.delete()
      resampleWarpAppOptions.delete()
      reprojectWarpAppOptions.delete()
    }

    it("raster sources optimized transformations should behave in a same way as a single warp application") {
      val base = filePath

      val optimizedRawResample = dsresampleOpt(base)

      val reprojectWarpAppOptions = new WarpOptions(new java.util.Vector(reprojectOptions.toWarpOptionsList.asJava))
      val resampleWarpAppOptions = new WarpOptions(new java.util.Vector(resampleOptions.toWarpOptionsList.asJava))
      val underlying = org.gdal.gdal.gdal.Open(filePath, org.gdal.gdalconst.gdalconstConstants.GA_ReadOnly)
      val reprojected = org.gdal.gdal.gdal.Warp("/dev/null", Array(underlying), reprojectWarpAppOptions)

      val originalRawResample = org.gdal.gdal.gdal.Warp("/dev/null", Array(reprojected), resampleWarpAppOptions)

      val rs =
        GDALRasterSource(filePath)
          .reproject(
            targetCRS      = WebMercator,
            resampleTarget = TargetCellSize(CellSize(10, 10)),
            strategy       = OverviewStrategy.DEFAULT
        )
        .resampleToRegion(
          region = GridExtent(Extent(-8769160.0, 4257700.0, -8750640.0, 4274460.0), CellSize(22, 22))
        )

      optimizedRawResample.gridExtent shouldBe rs.gridExtent
      datasetToRasterExtent(originalRawResample) shouldBe rs.gridExtent.toRasterExtent

      // cleanup JNI objects
      originalRawResample.delete()
      reprojected.delete()
      underlying.delete()
      resampleWarpAppOptions.delete()
      reprojectWarpAppOptions.delete()
    }
  }

}

object GDALWarpOptionsSpec {
  val lccProjection = CRS.fromString("+proj=lcc +lat_1=36.16666666666666 +lat_2=34.33333333333334 +lat_0=33.75 +lon_0=-79 +x_0=609601.22 +y_0=0 +datum=NAD83 +units=m +no_defs ")

  def generateWarpOptions(
    et: Option[Double] = 0.125.some,
    cellSize: Option[CellSize] = CellSize(19.1, 19.1).some,
    sourceCRS: Option[CRS] = lccProjection.some,
    targetCRS: Option[CRS] = WebMercator.some,
    te: Option[Extent] = None
  ): GDALWarpOptions = {
    GDALWarpOptions(
      Some("VRT"),
      Some(NearestNeighbor),
      et,
      cellSize,
      true,
      None,
      sourceCRS,
      targetCRS,
      te,
      None,
      Nil,
      Nil,
      Some(OverviewStrategy.DEFAULT),
      Nil, false, None, false, false, false, None, Nil, None, None, false, false,
      false, None, false, false, Nil, None, None, None, None, None, false, false, false, None, false, Nil, Nil, Nil, None
    )
  }
}
