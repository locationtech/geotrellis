/*
 * Copyright 2017 Azavea
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

package geotrellis.raster.reproject

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.raster.testkit._
import geotrellis.vector._

import org.scalatest._

class RasterizeReprojectSpec extends FunSpec
    with RasterMatchers {

  describe("Rasterizing reprojection") {
    it("should fill in only valid region") {
      val coloRaster: ProjectedRaster[Tile] = ProjectedRaster.apply(Raster[Tile](IntArrayTile.fill(1,700,400), Extent(-109,37,-102,41)), LatLng)
      val destRegion = coloRaster.projectedExtent.reprojectAsPolygon(ConusAlbers, 0.005)
      val destRE = ProjectedRasterExtent(destRegion.extent, ConusAlbers, 1000, 800)
      // =================================================
      // TODO: figure out why this causes a stack overflow
      val reprojected = coloRaster.reproject(ConusAlbers, TargetGridExtent(destRE.toGridType[Long]))
      // =================================================
      val trans = Proj4Transform(ConusAlbers, LatLng)

      // Must ignore narrow strip near boundary of tile (resampler issues)
      val ex = coloRaster.extent.buffer(-1e-3)
      var valid = true

      val errTile = IntArrayTile.ofDim(reprojected.cols, reprojected.rows)

      reprojected.tile.foreach{ (px, py, v) =>
        val (x, y) = reprojected.raster.rasterExtent.gridToMap(px, py)
        val (tx, ty) = trans(x, y)
        val dist = ex.distance(Point(tx, ty))
        if (dist > 1.2e-3) {
          valid = valid && (v != 1)
          v match {
            case 1 => errTile.set(px, py, -1)
            case _ => errTile.set(px, py, 1)
          }
        } else if (dist == 0) {
          valid = valid && (v == 1)
          v match {
            case 1 => errTile.set(px, py, 1)
            case _ => errTile.set(px, py, -1)
          }
        } else {
          // we ignored these pixels to avoid spurious errors due to resampling
          // near the original tile boundaries
          errTile.set(px, py, 0)
        }
      }

      // import geotrellis.raster.render._
      // val cm = ColorMap(Map( -1 -> 0xff0000ff, 0 -> 0x000000ff, 1 -> 0x00ff00ff))
      // errTile.renderPng(cm).write("colo.png")

      valid should be (true)
    }
  }

}
