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
import geotrellis.raster.merge._
import geotrellis.vector._
import geotrellis.proj4._

import spire.syntax.cfor._
import cats._


trait RasterRegionReproject[T <: CellGrid[Int]] extends Serializable {
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
    * @param errorThreshold error threshold when using approximate row transformations in reprojection.
    */
  def regionReproject(raster: Raster[T], src: CRS, dest: CRS, rasterExtent: RasterExtent, region: Polygon, resampleMethod: ResampleMethod, errorThreshold: Double): Raster[T]

  def regionReproject(raster: Raster[T], src: CRS, dest: CRS, rasterExtent: RasterExtent, region: Polygon, resampleMethod: ResampleMethod): Raster[T] =
    regionReproject(raster, src, dest, rasterExtent, region, resampleMethod, 0.125) // This default comes from GDAL 1.11 code


  /** Reproject raster to a region that may partially intersect target raster extent.
    *
    * Back-projects only the cells overlapping the destination region before sampling their value from the source raster.
    * This can be used to avoid assigning destination cell values where there is insufficient data for resample kernel.
    *
    * This is a <b>mutable</b> version of the region reproject. Target raster will be converted to mutable form and updated.
    * In cases where target raster is subclass [[MutableArrayTile]] its values will be mutated.
    * Its the responsability of the caller to use this function only when possible update in place is safe.\
    * Correct use of this function allows to avoid intermidate memory allocation when accumulating results of multiple reproject calls.
    *
    * @param raster source raster to be sampled for reprojection.
    * @param src source raster CRS
    * @param dest target raster CRS
    * @param target target raster that will be updated with pixels from source raster
    * @param region polygon boundry of source raster in target CRS
    * @param resampleMethod cell value resample method
    * @param errorThreshold error threshold when using approximate row transformations in reprojection.
    */
  def regionReprojectMutable(raster: Raster[T], src: CRS, dest: CRS, target: Raster[T], region: Polygon, resampleMethod: ResampleMethod, errorThreshold: Double): Raster[T]

  def regionReprojectMutable(raster: Raster[T], src: CRS, dest: CRS, target: Raster[T], region: Polygon, resampleMethod: ResampleMethod): Raster[T] =
    regionReprojectMutable(raster, src, dest, target, region, resampleMethod, 0.125) // This default comes from GDAL 1.11 code

  @deprecated("Use regionReprojectMerge instead", "2.1")
  def mutableRegionReproject(target: T, raster: Raster[T], src: CRS, dest: CRS, rasterExtent: RasterExtent, region: Polygon, resampleMethod: ResampleMethod): Unit = {
    // method kept for binary compatibility only, relies on possible mutable side effects
    regionReprojectMutable(raster, src, dest, Raster(target, rasterExtent.extent), region, resampleMethod)
  }
}

object RasterRegionReproject {
  private def rowCoords(
    destRegion: Polygon,
    destRasterExtent: RasterExtent,
    toSrcCrs: Transform,
    errorThreshold: Double
  ): Int => (Array[Int], Array[Double], Array[Double]) = {
    val extent = destRasterExtent.extent

    val rowTransform: RowTransform =
      if (errorThreshold != 0.0)
        RowTransform.approximate(toSrcCrs, errorThreshold)
      else
        RowTransform.exact(toSrcCrs)

    def scanlineCols(xmin: Double, xmax: Double): (Array[Int], Array[Double]) = {
      val x0 = ((xmin - extent.xmin) / destRasterExtent.cellwidth + 0.5 - 1e-6).toInt
      val x1 = ((xmax - extent.xmin) / destRasterExtent.cellwidth + 0.5 + 1e-6).toInt - 1
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
        val scanline = LineString(destRasterExtent.gridToMap(0, i), destRasterExtent.gridToMap(destRasterExtent.cols - 1, i))
        val chunks = (scanline & destRegion).toGeometry match {
          case None => Array.empty[Geometry]
          case Some(g) =>
            if (g.isInstanceOf[GeometryCollection])
              g.asInstanceOf[GeometryCollection].geometries.toArray
            else if (g.isInstanceOf[LineString])
              Array(g.asInstanceOf[LineString])
            else if (g.isInstanceOf[Point])
              Array(g.asInstanceOf[Point])
            else if (g.isInstanceOf[MultiLineString])
              g.asInstanceOf[MultiLineString].lines
            else if (g.isInstanceOf[MultiPoint])
              g.asInstanceOf[MultiPoint].points
            else
              throw new IllegalStateException("Line/polygon intersection may only produce a set of Lines and Points")
        }

        (for (chunk <- chunks) yield {
          chunk match {
            case l: LineString =>
              val (pxarr, xarr) = scanlineCols(l.getStartPoint.x, l.getEndPoint.x)
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
    def regionReproject(
      raster: Raster[Tile],
      src: CRS,
      dest: CRS,
      rasterExtent: RasterExtent,
      region: Polygon,
      resampleMethod: ResampleMethod,
      errorThreshold: Double
    ): Raster[Tile] = {
      val buffer = ArrayTile.empty(raster.tile.cellType, rasterExtent.cols, rasterExtent.rows)
      reprojectToBuffer(raster, src, dest, buffer, rasterExtent, region, resampleMethod, errorThreshold)
      Raster(buffer, rasterExtent.extent)
    }

    def regionReprojectMutable(
      raster: Raster[Tile],
      src: CRS,
      dest: CRS,
      target: Raster[Tile],
      region: Polygon,
      resampleMethod: ResampleMethod,
      errorThreshold: Double
    ): Raster[Tile] = {
      val buffer: MutableArrayTile = target.tile.mutable
      val bufferRE = target.rasterExtent
      reprojectToBuffer(raster, src, dest, buffer, bufferRE, region, resampleMethod, errorThreshold)
      Raster(buffer, bufferRE.extent)
    }

    private def reprojectToBuffer(
      raster: Raster[Tile],
      src: CRS,
      dest: CRS,
      target: MutableArrayTile,
      rasterExtent: RasterExtent,
      region: Polygon,
      resampleMethod: ResampleMethod,
      errorThreshold: Double
    ): Unit = {
      val trans = Proj4Transform(dest, src)
      val resampler = Resample(resampleMethod, raster.tile, raster.extent, CellSize(raster.rasterExtent.cellwidth, raster.rasterExtent.cellheight))
      val rowcoords = rowCoords(region, rasterExtent, trans, errorThreshold)

      if (raster.cellType.isFloatingPoint) {
        cfor(0)(_ < rasterExtent.rows, _ + 1){ i =>
          val (pxs, xs, ys) = rowcoords(i)
          cfor(0)(_ < xs.size, _ + 1) { s =>
            target.setDouble(pxs(s), i, resampler.resampleDouble(xs(s), ys(s)))
          }
        }
      } else {
        cfor(0)(_ < rasterExtent.rows, _ + 1){ i =>
          val (pxs, xs, ys) = rowcoords(i)
          cfor(0)(_ < xs.size, _ + 1) { s =>
            target.set(pxs(s), i, resampler.resample(xs(s), ys(s)))
          }
        }
      }
    }
  }

  implicit val multibandInstance = new RasterRegionReproject[MultibandTile] {
    def regionReproject(
      raster: Raster[MultibandTile],
      src: CRS,
      dest: CRS,
      rasterExtent: RasterExtent,
      region: Polygon,
      resampleMethod: ResampleMethod,
      errorThreshold: Double
    ): Raster[MultibandTile] = {
      val bands = Array.ofDim[MutableArrayTile](raster.tile.bandCount)
      cfor(0)(_ < bands.length, _ + 1) { i =>
        bands(i) = ArrayTile.empty(raster.tile.band(i).cellType, rasterExtent.cols, rasterExtent.rows)
      }
      reprojectToBuffer(raster, src, dest, bands, rasterExtent, region, resampleMethod, errorThreshold)
      Raster(MultibandTile(bands), rasterExtent.extent)
    }

    def regionReprojectMutable(
      raster: Raster[MultibandTile],
      src: CRS,
      dest: CRS,
      target: Raster[MultibandTile],
      region: Polygon,
      resampleMethod: ResampleMethod,
      errorThreshold: Double
    ): Raster[MultibandTile] = {
      val bufferRE = target.rasterExtent
      val bands = Array.ofDim[MutableArrayTile](raster.tile.bandCount)
      cfor(0)(_ < bands.length, _ + 1) { i =>
        bands(i) = target.tile.band(i).mutable
      }

      reprojectToBuffer(raster, src, dest, bands, bufferRE, region, resampleMethod, errorThreshold)
      Raster(MultibandTile(bands), bufferRE.extent)
    }

    private def reprojectToBuffer(
      raster: Raster[MultibandTile],
      src: CRS,
      dest: CRS,
      buffer: Array[MutableArrayTile],
      rasterExtent: RasterExtent,
      region: Polygon,
      resampleMethod: ResampleMethod,
      errorThreshold: Double
    ): Unit = {
      val trans = Proj4Transform(dest, src)
      val rowcoords = rowCoords(region, rasterExtent, trans, errorThreshold)
      val resampler: Array[Resample] = Array.ofDim[Resample](raster.tile.bandCount)

      for (b <- 0 until raster.tile.bandCount) {
        val band: Tile = raster.tile.bands(b)
        resampler(b) = Resample(resampleMethod, band, raster.extent, raster.rasterExtent.cellSize)
      }

      if (raster.cellType.isFloatingPoint) {
        cfor(0)(_ < rasterExtent.rows, _ + 1){ i =>
          val (pxs, xs, ys) = rowcoords(i)
          cfor(0)(_ < xs.size, _ + 1) { s =>
            cfor(0)(_ < buffer.length, _ + 1) { b =>
              buffer(b).setDouble(pxs(s), i, resampler(b).resampleDouble(xs(s), ys(s)))
            }
          }
        }
      } else {
        cfor(0)(_ < rasterExtent.rows, _ + 1){ i =>
          val (pxs, xs, ys) = rowcoords(i)
          cfor(0)(_ < xs.size, _ + 1) { s =>
            cfor(0)(_ < buffer.length, _ + 1) { b =>
              buffer(b).set(pxs(s), i, resampler(b).resample(xs(s), ys(s)))
            }
          }
        }
      }
    }
  }

  implicit def TileFeatureRasterRegionReproject[T <: CellGrid[Int] : RasterRegionReproject, D: Monoid](implicit ev: T => TileMergeMethods[T]) =
    new RasterRegionReproject[TileFeature[T, D]] {
      def regionReproject(
        raster: Raster[TileFeature[T, D]],
        src: CRS,
        dest: CRS,
        rasterExtent: RasterExtent,
        region: Polygon,
        resampleMethod: ResampleMethod,
        errorThreshold: Double
      ): Raster[TileFeature[T, D]] = {
        val srcRaster: Raster[T] = raster.mapTile(_.tile)
        val reprojected = implicitly[RasterRegionReproject[T]].regionReproject(srcRaster, src, dest, rasterExtent, region, resampleMethod, errorThreshold)
        new Raster[TileFeature[T, D]](TileFeature(reprojected.tile, raster.tile.data), raster.extent)
      }

      def regionReprojectMutable(
        raster: Raster[TileFeature[T, D]],
        src: CRS,
        dest: CRS,
        target: Raster[TileFeature[T, D]],
        region: Polygon,
        resampleMethod: ResampleMethod,
        errorThreshold: Double
      ): Raster[TileFeature[T, D]] = {
        val srcRaster: Raster[T] = raster.mapTile(_.tile)
        val dstRaster: Raster[T] = target.mapTile(_.tile)
        val reprojected: Raster[T] = implicitly[RasterRegionReproject[T]].regionReprojectMutable(srcRaster, src, dest, dstRaster, region, resampleMethod, errorThreshold)
        val mergedData = Monoid[D].combine(target.tile.data, raster.tile.data)
        reprojected.mapTile(TileFeature(_, mergedData))
      }
    }
}
