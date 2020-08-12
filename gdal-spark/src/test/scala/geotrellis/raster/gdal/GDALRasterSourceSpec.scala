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

import geotrellis.layer._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.resample._
import geotrellis.vector.ProjectedExtent
import geotrellis.raster.testkit._

import org.scalatest.GivenWhenThen
import org.scalatest.funspec.AnyFunSpec

class GDALRasterSourceSpec extends AnyFunSpec with RasterMatchers with GivenWhenThen {
  import geotrellis.GDALTestUtils._
  val uri = gdalGeoTiffPath("vlm/aspect-tiled.tif")

  describe("should perform a tileToLayout of a GeoTiffRasterSource") {

    // we are going to use this source for resampling into weird resolutions, let's check it
    // usually we align pixels
    lazy val source: GDALRasterSource = GDALRasterSource(uri, GDALWarpOptions(alignTargetPixels = false))

    val cellSizes = {
      val tiff = GeoTiffReader.readMultiband(uri)
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
      }
    }
  }
}
