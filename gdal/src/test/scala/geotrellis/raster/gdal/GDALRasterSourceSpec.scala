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

import cats.data.NonEmptyList
import geotrellis.proj4.{CRS, LatLng, WebMercator}
import geotrellis.raster.geotiff.GeoTiffRasterSource
import geotrellis.raster._
import geotrellis.raster.io.geotiff.{AutoHigherResolution, GeoTiff}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.resample._
import geotrellis.raster.testkit._
import geotrellis.vector.Extent

import org.scalatest.GivenWhenThen
import org.scalatest.funspec.AnyFunSpec

class GDALRasterSourceSpec extends AnyFunSpec with RasterMatchers with GivenWhenThen {

  val uri = Resource.path("vlm/aspect-tiled.tif")

  describe("GDALRasterSource") {

    // we are going to use this source for resampling into weird resolutions, let's check it
    // usually we align pixels
    lazy val source: GDALRasterSource = GDALRasterSource(uri, GDALWarpOptions(alignTargetPixels = false))

    it("should be able to read upper left corner") {
      val bounds = GridBounds(0, 0, 10, 10).toGridType[Long]
      val chip: Raster[MultibandTile] = source.read(bounds).get
      chip should have (
        // dimensions (bounds.width, bounds.height),
        cellType (source.cellType)
      )
    }

    it("should be able to resample") {
      // read in the whole file and resample the pixels in memory
      val etiff = GeoTiffReader.readMultiband(uri, streaming = false)
      val expected: Raster[MultibandTile] =
        etiff
          .raster
          .resample((source.cols * 0.95).toInt , (source.rows * 0.95).toInt, NearestNeighbor)
      // resample to 0.9 so we RasterSource picks the base layer and not an overview

      val resampledSource =
        source.resample(expected.tile.cols, expected.tile.rows, NearestNeighbor)

      // resampledSource should have (dimensions (expected.tile.dimensions))

      info(s"Source CellSize: ${source.cellSize}")
      info(s"Target CellSize: ${resampledSource.cellSize}")

      source.resolutions.length shouldBe (etiff.overviews.length + 1)
      source.resolutions.length shouldBe resampledSource.resolutions.length

      source.resolutions.length shouldBe (etiff.overviews.length + 1)
      source.resolutions.length shouldBe resampledSource.resolutions.length

      resampledSource.resolutions.zip(source.resolutions).map { case (rea, ree) =>
        rea.resolution shouldBe ree.resolution +- 1e-7
      }

      val actual: Raster[MultibandTile] =
        resampledSource.read(GridBounds(0, 0, resampledSource.cols - 1, resampledSource.rows - 1)).get

      withGeoTiffClue(actual, expected, resampledSource.crs) {
        assertRastersEqual(actual, expected)
      }
    }

    it("should not read past file edges") {
      Given("bounds larger than raster")
      val bounds = GridBounds(0, 0, source.cols + 100, source.rows + 100)
      When("reading by pixel bounds")
      val chip = source.read(bounds).get
      val expected = source.read(source.extent)

      Then("return only pixels that exist")
      // chip.tile should have (dimensions (source.dimensions))

      // check also that the tile is valid
      withGeoTiffClue(chip, expected.get, source.crs)  {
        assertRastersEqual(chip, expected.get)
      }
    }

    it("should derive a consistent extent") {
      GDALRasterSource(uri).extent should be (GeoTiffRasterSource(uri).extent)

      val p = Resource.path("vlm/extent-bug.tif")
      GDALRasterSource(GDALPath(p)).extent should be (GeoTiffRasterSource(p).extent)
    }

    it("should not fail on creation of the GDALRasterSource on a 'malformed URI', since we don't know if it is a path or it is a scheme") {
      val result = GDALRasterSource("file:/random/path/here/N49W155.hgt.gz")
      result.path shouldBe "/vsigzip/file:/random/path/here/N49W155.hgt.gz"
    }

    it("should read the same metadata as GeoTiffRasterSource") {
      lazy val tsource = GeoTiffRasterSource(uri)
      source.metadata.attributes.mapValues(_.toUpperCase) shouldBe tsource.metadata.attributes.mapValues(_.toUpperCase)
    }

    it("should perform a chained reprojection") {
      val rs = GDALRasterSource(Resource.path("vlm/lc8-utm-1.tif"))
      val drs = rs.reproject(WebMercator).reproject(LatLng).asInstanceOf[GDALRasterSource]
      val rrs = rs.reproject(LatLng).asInstanceOf[GDALRasterSource]

      drs.crs shouldBe rrs.crs

      val RasterExtent(Extent(dxmin, dymin, dxmax, dymax), dcw, dch, dc, dr) = drs.gridExtent.toRasterExtent()
      val RasterExtent(Extent(rxmin, rymin, rxmax, rymax), rcw, rch, rc, rr) = rrs.gridExtent.toRasterExtent()

      // our reprojection rounding error
      // to fix it we can round cellSize in the options reprojection by 6 numbers after the decimal point
      dxmin shouldBe rxmin +- 1e-1
      dymin shouldBe rymin +- 1e-1
      dxmax shouldBe rxmax +- 1e-1
      dymax shouldBe rymax +- 1e-1

      dcw shouldBe rcw +- 1e-6
      dch shouldBe rch +- 1e-6

      dc shouldBe rc +- 1
      dr shouldBe rr +- 1

      // manually ensure that the difference in two GDALWarpOptions
      drs.options.copy(cellSize = None, te = None) shouldBe rrs.options.copy(cellSize = None, te = None)
      val CellSize(docw, doch) = drs.options.cellSize.get
      val CellSize(rocw, roch) = rrs.options.cellSize.get
      docw shouldBe rocw +- 1e-6
      doch shouldBe roch +- 1e-6

      val Extent(dtexmin, dteymin, dtexmax, dteymax) = drs.options.te.get
      val Extent(rtexmin, rteymin, rtexmax, rteymax) = rrs.options.te.get

      dtexmin shouldBe rtexmin +- 1e-1
      dteymin shouldBe rteymin +- 1e-1
      dtexmax shouldBe rtexmax +- 1e-1
      dteymax shouldBe rteymax +- 1e-1
    }

    describe("should derive the cellType consistently with GeoTiffRasterSource") {
      val raster = Raster(
        ArrayTile(Array(
          1, 2, 3,
          4, 5, 6,
          7, 8, 9
        ), 3, 3),
        Extent(0.0, 0.0, 3.0, 3.0)
      )

      List(
        BitCellType,
        ByteCellType,
        ByteConstantNoDataCellType,
        ByteUserDefinedNoDataCellType((Byte.MinValue + 1).toByte),
        UByteCellType,
        UByteConstantNoDataCellType,
        UByteUserDefinedNoDataCellType(1),
        ShortCellType,
        ShortConstantNoDataCellType,
        ShortUserDefinedNoDataCellType((Short.MinValue + 1).toShort),
        UShortCellType,
        UShortConstantNoDataCellType,
        UShortUserDefinedNoDataCellType(1),
        IntCellType,
        IntConstantNoDataCellType,
        IntUserDefinedNoDataCellType(Int.MinValue + 1),
        FloatCellType,
        FloatConstantNoDataCellType,
        FloatUserDefinedNoDataCellType(Float.MinValue + 1),
        DoubleCellType,
        DoubleConstantNoDataCellType,
        DoubleUserDefinedNoDataCellType(Double.MinValue + 1)
      ) map { ct =>
        it(ct.getClass.getName.split("\\$").last.split("\\.").last) {
          val path = s"/tmp/gdal-$ct-test.tiff"
          val craster = raster.mapTile(_.convert(ct))
          GeoTiff(craster, LatLng).write(path)

          GDALRasterSource(path).cellType shouldBe ct
          GeoTiffRasterSource(path).cellType shouldBe ct
        }
      }
    }


    it("reprojectToRegion should produce an expected raster") {
      val expected = GeoTiffRasterSource(Resource.path("vlm/lc8-utm-re-mosaic-expected.tif")).read().get

      val uris = List(
        Resource.path("vlm/lc8-utm-1.tif"),
        Resource.path("vlm/lc8-utm-2.tif"),
        Resource.path("vlm/lc8-utm-3.tif")
      )

      val targetGridExtent = GridExtent[Long](
        Extent(-89.18876450968908, 37.796833507913995, -73.9486554460876, 41.41061537114088),
        CellSize(0.01272458402544677,0.01272129304140357)
      )
      val e = Extent(-89.18876450968908, 37.796833507913995, -73.9486554460876, 41.41061537114088)
      val targetCRS = CRS.fromEpsgCode(4326)

      val rasterSources = NonEmptyList.fromListUnsafe(uris.map(GDALRasterSource(_)))
      val moisac = MosaicRasterSource.instance(rasterSources, rasterSources.head.crs)

      val actual =
        moisac
          .reprojectToRegion(
            targetCRS,
            targetGridExtent.toRasterExtent,
            NearestNeighbor,
            AutoHigherResolution
          )
          .read(e)
          .get

      assertEqual(actual, expected)
    }
  }
}
