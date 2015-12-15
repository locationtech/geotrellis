package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.vector.Extent
import geotrellis.proj4._

import spire.syntax.cfor._

case class ReprojectOptions(method: ResampleMethod = NearestNeighbor, errorThreshold: Double = 0.125)
object ReprojectOptions {
  val DEFAULT = ReprojectOptions()
}

object Reproject {
  def apply(tile: Tile, extent: Extent, src: CRS, dest: CRS, options: ReprojectOptions): Raster =
    if(src == dest) {
      (tile.toArrayTile.copy, extent)
    } else {
      val re = RasterExtent(extent, tile.cols, tile.rows)
      val cellwidth = re.cellwidth
      val cellheight = re.cellheight

      val transform = Transform(src, dest)
      val newRe @ RasterExtent(newExtent, newCellWidth, newCellHeight, newCols, newRows) =
        ReprojectRasterExtent(re, transform)

      val newTile = ArrayTile.empty(tile.cellType, newCols, newRows)

      val inverseTransform = Transform(dest, src)

      val rowTransform: RowTransform =
        if (options.errorThreshold != 0.0)
          RowTransform.approximate(inverseTransform, options.errorThreshold)
        else
          RowTransform.exact(inverseTransform)

      // The map coordinates of the destination raster
      val (topLeftX, topLeftY) = newRe.gridToMap(0,0)
      val destX = Array.ofDim[Double](newCols)
      var currX = topLeftX
      cfor(0)(_ < newCols, _ + 1) { i =>
        destX(i) = currX
        currX += newCellWidth
      }

      val destY = Array.ofDim[Double](newCols).fill(topLeftY)


      // The map coordinates of the source raster, transformed from the
      // destination map coordinates on each row iteration
      val srcX = Array.ofDim[Double](newCols)
      val srcY = Array.ofDim[Double](newCols)

      val resampler = Resample(options.method, tile, extent, newRe.cellSize)

      if(tile.cellType.isFloatingPoint) {
        val resample = resampler.resampleDouble _
        cfor(0)(_ < newRows, _ + 1) { row =>
          // Reproject this whole row.
          rowTransform(destX, destY, srcX, srcY)
          cfor(0)(_ < newCols, _ + 1) { col =>
            val v = resample(srcX(col), srcY(col))
            newTile.setDouble(col, row, v)

            // Add row height for next iteration
            destY(col) -= newCellHeight
          }
        }
      } else {
        val resample = resampler.resample _
        cfor(0)(_ < newRows, _ + 1) { row =>
          // Reproject this whole row.
          rowTransform(destX, destY, srcX, srcY)
          cfor(0)(_ < newCols, _ + 1) { col =>
            val x = srcX(col)
            val y = srcY(col)

            val v = resample(x, y)
            newTile.set(col, row, v)

            // Add row height for next iteration
            destY(col) -= newCellHeight
          }
        }
      }

      Raster(newTile, newExtent)
    }

  def apply(tile: MultiBandTile, extent: Extent, src: CRS, dest: CRS, options: ReprojectOptions): MultiBandRaster = {
    val bands =
      for ( bandIndex <- 0 until tile.bandCount ) yield {
        tile.band(bandIndex).reproject(extent, src, dest, options)
      }

    MultiBandRaster(ArrayMultiBandTile(bands.map(_.tile)), bands.head.extent)
  }
}
