package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.vector.Extent
import geotrellis.proj4._

import spire.syntax.cfor._

object SingleBandReproject {
  type Apply = Reproject.Apply[Raster]

  class CallApply(
    raster: Raster,
    targetRasterExtent: RasterExtent,
    transform: Transform,
    inverseTransform: Transform
  ) extends Apply {
    def apply(method: ResampleMethod = NearestNeighbor, errorThreshold: Double = 0.125): Raster = {
      val Raster(tile, extent) = raster
      val RasterExtent(_, cellwidth, cellheight, _, _) = raster.rasterExtent
      val RasterExtent(newExtent, newCellWidth, newCellHeight, newCols, newRows) = targetRasterExtent

      val newTile = ArrayTile.empty(tile.cellType, newCols, newRows)

      val rowTransform: RowTransform =
        if (errorThreshold != 0.0)
          RowTransform.approximate(inverseTransform, errorThreshold)
        else
          RowTransform.exact(inverseTransform)

      // The map coordinates of the destination raster
      val (topLeftX, topLeftY) = targetRasterExtent.gridToMap(0,0)
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

      val resampler = Resample(method, tile, extent, CellSize(newCellWidth, newCellHeight))

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
  }

  def apply(tile: Tile, extent: Extent, src: CRS, dest: CRS): Apply =
    apply(Raster(tile, extent), src, dest)

  def apply(raster: Raster, src: CRS, dest: CRS): Apply =
    if(src == dest) {
      new Reproject.NoOpApply(raster)
    } else {
      val transform = Transform(src, dest)
      val inverseTransform = Transform(dest, src)

      val targetRasterExtent = ReprojectRasterExtent(raster.rasterExtent, transform)

      apply(raster, targetRasterExtent, transform, inverseTransform)
    }

  /** Windowed */
  def apply(gridBounds: GridBounds, tile: Tile, extent: Extent, src: CRS, dest: CRS): Apply =
    apply(gridBounds, Raster(tile, extent), src, dest)

  /** Windowed */
  def apply(gridBounds: GridBounds, raster: Raster, src: CRS, dest: CRS): Apply = {
    val transform = Transform(src, dest)
    val inverseTransform = Transform(dest, src)

    apply(gridBounds, raster, transform, inverseTransform)
  }

  def apply(gridBounds: GridBounds, raster: Raster, transform: Transform, inverseTransform: Transform): Apply = {
    val rasterExtent = raster.rasterExtent
    val windowExtent = rasterExtent.extentFor(gridBounds)

    val windowRasterExtent = RasterExtent(windowExtent, gridBounds.width, gridBounds.height)
    val targetRasterExtent = ReprojectRasterExtent(windowRasterExtent, transform)

    apply(raster, targetRasterExtent, transform, inverseTransform)
  }

  def apply(raster: Raster, targetRasterExtent: RasterExtent, transform: Transform, inverseTransform: Transform): Apply =
    new CallApply(raster, targetRasterExtent, transform, inverseTransform)
}
