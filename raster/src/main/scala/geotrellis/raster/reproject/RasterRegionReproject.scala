package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.raster.rasterize._
import geotrellis.vector.Polygon
import geotrellis.proj4._

import spire.syntax.cfor._

trait RasterRegionReproject[T <: CellGrid]{
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
}

object RasterRegionReproject {
  implicit val singlebandInstance = new RasterRegionReproject[Tile] {
    def regionReproject(raster: Raster[Tile], src: CRS, dest: CRS, rasterExtent: RasterExtent, region: Polygon, resampleMethod: ResampleMethod): Raster[Tile] = {
      val buffer = raster.tile.prototype(rasterExtent.cols, rasterExtent.rows)
      val trans = Proj4Transform(dest, src)
      val resampler = Resample.apply(resampleMethod, raster.tile, raster.extent, CellSize(raster.rasterExtent.cellwidth, raster.rasterExtent.cellheight))

      if (raster.cellType.isFloatingPoint) {
        Rasterizer.foreachCellByPolygon(region, rasterExtent) { (px, py) =>
          val (x, y) = rasterExtent.gridToMap(px, py)
          val (tx, ty) = trans(x, y)
          buffer.setDouble(px, py, resampler.resampleDouble(tx, ty))
        }
      } else {
        Rasterizer.foreachCellByPolygon(region, rasterExtent) { (px, py) =>
          val (x, y) = rasterExtent.gridToMap(px, py)
          val (tx, ty) = trans(x, y)
          buffer.set(px, py, resampler.resample(tx, ty))
        }
      }

      Raster(buffer, rasterExtent.extent)
    }
  }

  implicit val multibandInstance = new RasterRegionReproject[MultibandTile] {
    def regionReproject(raster: Raster[MultibandTile], src: CRS, dest: CRS, rasterExtent: RasterExtent, region: Polygon, resampleMethod: ResampleMethod): Raster[MultibandTile] = {
      val trans = Proj4Transform(dest, src)
      val bands = Array.ofDim[MutableArrayTile](raster.tile.bandCount)

      cfor(0)(_ < bands.length, _ + 1) { i =>
        bands(i) = raster.band(i).prototype(rasterExtent.cols, rasterExtent.rows).mutable
      }

      val resampler = (0 until raster.bandCount).map { i =>
        Resample(resampleMethod, raster.band(i), raster.extent, raster.rasterExtent.cellSize)
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
