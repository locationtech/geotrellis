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

package geotrellis.layer

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.testkit.{RasterMatchers, Resource}
import geotrellis.raster.geotiff._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.resample._
import geotrellis.vector._

import java.io.File

import org.scalatest.funspec.AnyFunSpec

class LayoutTileSourceSpec extends AnyFunSpec with RasterMatchers {
  /** Function to get paths from the raster/data dir.  */
  def rasterGeoTiffPath(name: String): String = {
    def baseDataPath = "raster/data"
    val path = s"$baseDataPath/$name"
    require(new File(path).exists, s"$path does not exist, unzip the archive?")
    path
  }

  val testFile = rasterGeoTiffPath("vlm/aspect-tiled.tif")
  lazy val tiff = GeoTiffReader.readMultiband(testFile, streaming = false)

  lazy val rasterSource = GeoTiffRasterSource(testFile)
  val scheme = FloatingLayoutScheme(256)
  lazy val layout = scheme.levelFor(rasterSource.extent, rasterSource.cellSize).layout
  lazy val source = LayoutTileSource.spatial(rasterSource, layout)

  val mbTestFile = Resource.path("vlm/multiband.tif")
  lazy val mbTiff = GeoTiffReader.readMultiband(mbTestFile, streaming = false)
  lazy val mbRasterSource = GeoTiffRasterSource(mbTestFile)
  lazy val mbLayout = scheme.levelFor(mbRasterSource.extent, mbRasterSource.cellSize).layout
  lazy val mbSource = LayoutTileSource.spatial(mbRasterSource, mbLayout)

  it("should read all the keys") {
    val keys = source.layout
      .mapTransform
      .extentToBounds(rasterSource.extent)
      .coordsIter.toList

    for ((col, row) <- keys ) {
      val key = SpatialKey(col, row)
      val re = RasterExtent(
        extent = layout.mapTransform.keyToExtent(key),
        cellwidth = layout.cellwidth,
        cellheight = layout.cellheight,
        cols = layout.tileCols,
        rows = layout.tileRows)

      withClue(s"$key:") {
        val tile = source.read(key).get
        val actual = Raster(tile, re.extent)
        val expected = tiff.crop(rasterExtent = re)

        withGeoTiffClue(actual, expected, source.source.crs) {
          assertRastersEqual(actual, expected)
        }
      }
    }
  }

  it("should subset bands if requested") {
    val coord = mbSource.layout
      .mapTransform
      .extentToBounds(mbRasterSource.extent)
      .coordsIter.toList
      .head

    val key = SpatialKey(coord._1, coord._2)
    val re = RasterExtent(
      extent = mbLayout.mapTransform.keyToExtent(key),
      cellwidth = mbLayout.cellwidth,
      cellheight = mbLayout.cellheight,
      cols = mbLayout.tileCols,
      rows = mbLayout.tileRows
    )

    withClue(s"$key:") {
      val tile = mbSource.read(key, Seq(1, 2)).get
      val actual = Raster(tile, re.extent)
      val expected = Raster(
        mbTiff
          .crop(rasterExtent = re.copy(extent = re.extent.buffer(re.cellSize.resolution / 4)))
          .tile
          .subsetBands(1, 2),
        re.extent
      )
      withGeoTiffClue(actual, expected, mbSource.source.crs) {
        assertRastersEqual(actual, expected)
      }
    }
  }

  /** https://github.com/geotrellis/geotrellis-contrib/issues/116 */
  describe("should read by key on high zoom levels properly // see issue-116 in a geotrellis-contrib repo") {
    val crs: CRS = WebMercator

    val tmsLevels: Array[LayoutDefinition] = {
      val scheme = ZoomedLayoutScheme(crs, 256)
      for (zoom <- 0 to 64) yield scheme.levelForZoom(zoom).layout
    }.toArray

    val path = Resource.path("vlm/issue-116-cog.tif")
    val subsetBands = List(0, 1, 2)

    val layoutDefinition = tmsLevels(22)

    for(c <- 1249656 to 1249658; r <- 1520655 to 1520658) {
      it(s"reading 22/$c/$r") {
        val result =
          GeoTiffRasterSource(path)
            .reproject(WebMercator)
            .tileToLayout(layoutDefinition)
            .read(SpatialKey(c, r), subsetBands)
            .get


        result.bands.foreach { band =>
          (1 until band.rows).foreach { r =>
            band.getDouble(band.cols - 1, r) shouldNot be(0d)
          }
        }
      }
    }
  }

  describe("should perform a tileToLayout of a GeoTiffRasterSource") {
    lazy val url = testFile

    lazy val source: GeoTiffRasterSource = GeoTiffRasterSource(url)

    val cellSizes = {
      val tiff = GeoTiffReader.readMultiband(url)
      (tiff +: tiff.overviews).map(_.rasterExtent.cellSize).map { case CellSize(w, h) =>
        CellSize(w + 1, h + 1)
      }
    }

    cellSizes.foreach { targetCellSize =>
      it(s"for cellSize: $targetCellSize") {
        val pe = ProjectedExtent(source.extent, source.crs)
        val scheme = FloatingLayoutScheme(256)
        val layout = scheme.levelFor(pe.extent, targetCellSize).layout
        val mapTransform = layout.mapTransform
        val resampledSource = source.resampleToGrid(layout)

        val expected: List[(SpatialKey, MultibandTile)] =
          mapTransform(pe.extent).coordsIter.map { case (col, row) =>
            val key = SpatialKey(col, row)
            val ext = mapTransform.keyToExtent(key)
            val raster = resampledSource.read(ext).get
            val newTile = raster.tile.prototype(source.cellType, layout.tileCols, layout.tileRows)
            key -> newTile.merge(
              ext,
              raster.extent,
              raster.tile,
              NearestNeighbor
            )
          }.toList

        val layoutSource = source.tileToLayout(layout)
        val actual: List[(SpatialKey, MultibandTile)] = layoutSource.readAll().toList

        withClue(s"actual.size: ${actual.size} expected.size: ${expected.size}") {
          actual.size should be(expected.size)
        }

        val sortedActual: List[Raster[MultibandTile]] =
          actual
            .sortBy { case (k, _) => (k.col, k.row) }
            .map { case (k, v) => Raster(v, mapTransform.keyToExtent(k)) }

        val sortedExpected: List[Raster[MultibandTile]] =
          expected
            .sortBy { case (k, _) => (k.col, k.row) }
            .map { case (k, v) => Raster(v, mapTransform.keyToExtent(k)) }

        val grouped: List[(Raster[MultibandTile], Raster[MultibandTile])] =
          sortedActual.zip(sortedExpected)

        grouped.foreach { case (actualTile, expectedTile) =>
          withGeoTiffClue(actualTile, expectedTile, source.crs) {
            assertRastersEqual(actualTile, expectedTile)
          }
        }

        layoutSource.source
      }
    }
  }

  describe("should perform reproject and tileToLayout of a GeoTiffRasterSource") {
    // https://github.com/locationtech/geotrellis/issues/3267
    it("should read reprojected and tiled to layout source // see issue-3267") {
      val path = Resource.path("vlm/seams-16.tif")
      val testZoomLevel = 16
      val rs = GeoTiffRasterSource(path)

      val scheme = ZoomedLayoutScheme(WebMercator)
      val pyramid = (1 to 30).map { z => (z, scheme.levelForZoom(z)) }.toMap

      val extent = rs.extent.reproject(rs.crs, WebMercator)

      def getTileZXY(point: Point, zoom: Int) = {
        val SpatialKey(x, y) = pyramid(zoom).layout.mapTransform(point)
        (zoom, x, y)
      }

      val (_, x, y) = getTileZXY(extent.center, testZoomLevel)

      val neighborhood = for {
        col <- ((x - 2) until (x + 2)).toList
        row <- ((y - 2) until (y + 2)).toList
      } yield (col, row)

      val trs = rs
        .reproject(WebMercator, method = NearestNeighbor)
        .tileToLayout(scheme.levelForZoom(testZoomLevel).layout)

      neighborhood.map { key =>
        val t = trs.read(key).get.band(0)
        val arr = trs.read(key).get.band(0).toArray
        // info(s"Debug info for: ($key)")
        arr.sum shouldBe arr.size
        t.dimensions shouldBe Dimensions(256, 256)
      }
    }
  }
}
