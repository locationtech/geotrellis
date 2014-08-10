package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.proj4._

import spire.syntax.cfor._

case class ReprojectOptions(method: InterpolationMethod = Bilinear, errorThreshold: Double = 0.125)
object ReprojectOptions {
  val DEFAULT = ReprojectOptions()
}

object Reproject {
  def apply(tile: Tile, extent: Extent, src: CRS, dest: CRS)(implicit options: ReprojectOptions): (Tile, Extent) = {
    val re = RasterExtent(extent, tile.cols, tile.rows)

    val transform = Transform(src, dest)
    val newRe @ RasterExtent(newExtent, newCellWidth, newCellHeight, newCols, newRows) =
      ReprojectRasterExtent(re, transform)

    val newTile = ArrayTile.empty(tile.cellType, newCols, newRows)

    val inverseTransform = Transform(dest, src)

    val rowTransform: RowTransform =
      if(options.errorThreshold != 0.0)
        RowTransform.approximate(inverseTransform, options.errorThreshold)
      else
        RowTransform.exact(inverseTransform)

    // The map coordinates of the destination raster
    val destX = Array.ofDim[Double](newCols)
    val destY = Array.ofDim[Double](newCols)
    
    // The map coordinates of the source raster, transformed from the
    // destination map coordinates on each row iteration
    val srcX = Array.ofDim[Double](newCols)
    val srcY = Array.ofDim[Double](newCols)

    val interpolation = Interpolation(options.method, tile, extent)

    if(tile.cellType.isFloatingPoint) {
      val interpolate = interpolation.interpolateDouble _
      cfor(0)(_ < newRows, _ + 1) { row =>
        // Reproject this whole row.
        rowTransform(destX, destY, srcX, srcY)
        cfor(0)(_ < newCols, _ + 1) { col =>
          val v = interpolate(srcX(col), srcY(col))
          newTile.setDouble(col, row, v)

          // Add row height for next iteration
          destY(col) += newCellHeight
        }
      }
    } else {
      val interpolate = interpolation.interpolate _
      cfor(0)(_ < newRows, _ + 1) { row =>
        // Reproject this whole row.
        rowTransform(destX, destY, srcX, srcY)
        cfor(0)(_ < newCols, _ + 1) { col =>
          val v = interpolate(srcX(col), srcY(col))
          newTile.set(col, row, v)

          // Add row height for next iteration
          destY(col) += newCellHeight
        }
      }
    }

    (newTile, newExtent)
  }
}
