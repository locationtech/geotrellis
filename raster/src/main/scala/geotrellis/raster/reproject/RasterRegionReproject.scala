/*
 * Copyright 2018 Azavea
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

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.raster.rasterize._
import geotrellis.vector.{Geometry, GeometryCollection, Line, MultiLine, MultiPoint, Point, Polygon}
import geotrellis.proj4._

import spire.syntax.cfor._

trait RasterRegionReproject[T <: CellGrid] extends Serializable {
  /** Reproject raster to a region that may partially intersect target raster extent.
    *
    * Back-projects only the cells overlapping the destination region before sampling their value from the source raster.
    * This can be used to avoid assigning destination cell values where there is insufficient data for resample kernel.
    *
    * @param src source raster CRS
    * @param dest target raster CRS
    * @param rasterExtent extent and resolution of target raster
    * @param region polygon boundry of source raster in target CRS
    * @param resampleMethod cell value resample method
    */
  def regionReproject(raster: Raster[T], src: CRS, dest: CRS, rasterExtent: RasterExtent, region: Polygon, resampleMethod: ResampleMethod): Raster[T]

  def mutableRegionReproject(target: T, raster: Raster[T], src: CRS, dest: CRS, rasterExtent: RasterExtent, region: Polygon, resampleMethod: ResampleMethod): Unit

}

object RasterRegionReproject {
  private def rowCoords(destRegion: Polygon, destRasterExtent: RasterExtent, toSrcCrs: Transform): Int => (Array[Int], Array[Double], Array[Double]) = {
    val extent = destRasterExtent.extent
    val rowTransform = RowTransform.approximate(toSrcCrs, 0.125)

    def scanlineCols(xmin: Double, xmax: Double): (Array[Int], Array[Double]) = {
      val x0 = ((xmin - extent.xmin) / destRasterExtent.cellwidth + 0.5 - 1e-8).toInt
      val x1 = ((xmax - extent.xmin) / destRasterExtent.cellwidth + 0.5 + 1e-8).toInt - 1
      val locations = Array.ofDim[Int](x1 - x0 + 1)
      val result = Array.ofDim[Double](x1 - x0 + 1)
      cfor(x0)(_ <= x1, _ + 1){ i =>
        locations(i - x0) = i
        result(i - x0) = (i + 0.5)* destRasterExtent.cellwidth + extent.xmin
      }
      (locations, result)
    }

    { i: Int =>
      if (i >= 0 && i < destRasterExtent.rows) {
        val scanline = Line(destRasterExtent.gridToMap(0, i), destRasterExtent.gridToMap(destRasterExtent.cols - 1, i))
        val chunks = scanline.intersection(destRegion).toGeometry match {
          case None => Array.empty[Geometry]
          case Some(g) =>
            if (g.isInstanceOf[GeometryCollection])
              g.asInstanceOf[GeometryCollection].geometries.toArray
            else if (g.isInstanceOf[Line])
              Array(g.asInstanceOf[Line])
            else if (g.isInstanceOf[Point])
              Array(g.asInstanceOf[Point])
            else if (g.isInstanceOf[MultiLine])
              g.asInstanceOf[MultiLine].lines
            else if (g.isInstanceOf[MultiPoint])
              g.asInstanceOf[MultiPoint].points
            else
              throw new IllegalStateException("Line/polygon intersection may only produce a set of Lines and Points")
        }

        (for (chunk <- chunks) yield {
          chunk match {
            case l: Line =>
              val (pxarr, xarr) = scanlineCols(l.head.x, l.last.x)
              val yarr = Array.fill[Double](xarr.size)(destRasterExtent.gridRowToMap(i))
              val xres = Array.ofDim[Double](xarr.size)
              val yres = Array.ofDim[Double](xarr.size)

              if (xarr.size > 0)
                rowTransform(xarr, yarr, xres, yres)

              (pxarr, xres, yres)
            case p: Point =>
              val (x, y) = toSrcCrs(p.x, p.y)
              (Array(destRasterExtent.mapXToGrid(p.x)), Array(x), Array(y))
            case g: Geometry =>
              throw new IllegalStateException("Line-Polygon intersection cannot produce geometries that are not Points or Lines")
            }
        }).fold( (Array.empty[Int], Array.empty[Double], Array.empty[Double]) ){ (a1, a2) => {
          val (px1, x1, y1) = a1
          val (px2, x2, y2) = a2
          (px1 ++ px2, x1 ++ x2, y1 ++ y2)
        }}
      } else
        (Array.empty[Int], Array.empty[Double], Array.empty[Double])
    }
  }

  implicit val singlebandInstance = new RasterRegionReproject[Tile] {
    def regionReproject(raster: Raster[Tile], src: CRS, dest: CRS, rasterExtent: RasterExtent, region: Polygon, resampleMethod: ResampleMethod): Raster[Tile] = {
      val buffer = raster.tile.prototype(rasterExtent.cols, rasterExtent.rows)
      mutableRegionReproject(buffer, raster, src, dest, rasterExtent, region, resampleMethod)
      Raster(buffer, rasterExtent.extent)
    }

    def mutableRegionReproject(target: Tile, raster: Raster[Tile], src: CRS, dest: CRS, rasterExtent: RasterExtent, region: Polygon, resampleMethod: ResampleMethod) = {
      val buffer = target.mutable
      val trans = Proj4Transform(dest, src)
      val resampler = Resample.apply(resampleMethod, raster.tile, raster.extent, CellSize(raster.rasterExtent.cellwidth, raster.rasterExtent.cellheight))

      val rowcoords = rowCoords(region, rasterExtent, trans)

      cfor(0)(_ < rasterExtent.rows, _ + 1){ i =>
        val (pxs, xs, ys) = rowcoords(i)
        if (raster.cellType.isFloatingPoint) {
          cfor(0)(_ < xs.size, _ + 1){ s =>
            buffer.setDouble(pxs(s), i, resampler.resampleDouble(xs(s), ys(s)))
          }
        } else {
          cfor(0)(_ < xs.size, _ + 1){ s =>
            buffer.set(pxs(s), i, resampler.resample(xs(s), ys(s)))
          }
        }
      }
    }
  }

  implicit val multibandInstance = new RasterRegionReproject[MultibandTile] {
    def regionReproject(raster: Raster[MultibandTile], src: CRS, dest: CRS, rasterExtent: RasterExtent, region: Polygon, resampleMethod: ResampleMethod): Raster[MultibandTile] = {
      val bands = Array.ofDim[MutableArrayTile](raster.tile.bandCount)
      cfor(0)(_ < bands.length, _ + 1) { i =>
        bands(i) = raster.tile.band(i).prototype(rasterExtent.cols, rasterExtent.rows).mutable
      }
      mutableRegionReproject(MultibandTile(bands), raster, src, dest, rasterExtent, region, resampleMethod)
      Raster(MultibandTile(bands), rasterExtent.extent)
    }

    def mutableRegionReproject(target: MultibandTile, raster: Raster[MultibandTile], src: CRS, dest: CRS, rasterExtent: RasterExtent, region: Polygon, resampleMethod: ResampleMethod) = {
      val trans = Proj4Transform(dest, src)
      val bands = Array.ofDim[MutableArrayTile](raster.tile.bandCount)

      cfor(0)(_ < bands.length, _ + 1) { i =>
        bands(i) = target.band(i).mutable
      }

      val resampler = (0 until raster.tile.bandCount).map { i =>
        Resample(resampleMethod, raster.tile.band(i), raster.extent, raster.rasterExtent.cellSize)
      }

      if (raster.cellType.isFloatingPoint) {
        Rasterizer.foreachCellByPolygon(region, rasterExtent) { (px, py) =>
          val (x, y) = rasterExtent.gridToMap(px, py)
          val (tx, ty) = trans(x, y)
          cfor(0)(_ < bands.length, _ + 1) { i =>
            bands(i).setDouble(px, py, resampler(i).resampleDouble(tx, ty))
          }
        }
      } else {
        Rasterizer.foreachCellByPolygon(region, rasterExtent) { (px, py) =>
          val (x, y) = rasterExtent.gridToMap(px, py)
          val (tx, ty) = trans(x, y)
          cfor(0)(_ < bands.length, _ + 1) { i =>
            bands(i).set(px, py, resampler(i).resample(tx, ty))
          }
        }
      }

      Raster(MultibandTile(bands), rasterExtent.extent)
    }
  }
}
