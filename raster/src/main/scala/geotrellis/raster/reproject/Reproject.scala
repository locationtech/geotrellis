package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.proj4._

import spire.syntax.cfor._

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

      // For each target cell, reproject to source raster, take the interpolation of the values and set into the new raster.

      def interpolate(x: Double, y: Double): Double = ???

      // The map coordinates of the destination raster
      val destX = Array.ofDim[Double](newCols)
      val destY = Array.ofDim[Double](newCols)
      
      // The map coordinates of the source raster, transformed from the 
      // destination map coordinates on each row iteration
      val srcX = Array.ofDim[Double](newCols)
      val srcY = Array.ofDim[Double](newCols)

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


      // val xmin = extent.xmin + (re.cellwidth / 2.0)
      // val xmax = extent.xmax - (re.cellwidth / 2.0)
      // val ymin = extent.ymin + (re.cellheight / 2.0)
      // val ymax = extent.ymax - (re.cellheight / 2.0)
      // val cellwidth = re.cellwidth
      // val cellheight = re.cellheight

      //   val inverseDeltas = 1 / (cellwidth * cellheight)
      //   cfor(0)( _ < newRows, _ + 1) { row =>
      //     val (x, y) = newRe.gridToMap(col, row)
      //     val (sx, sy) = transform(x, y)

      //     if(extent.xmin <= sx || sx <= extent.xmax ||
      //        extent.ymin <= sy || sy <= extent.ymax) {

      //       // Bilinear
      //       val dleft = sx - xmin
      //       val leftCol = math.floor(dleft / re.cellwidth).toInt
      //       val leftX = xmin + (leftCol * cellwidth)

      //       val dright = xmax - sx
      //       val rightCol = leftCol + 1
      //       val rightX = leftX + cellwidth

      //       val dbottom = sy - ymin
      //       val bottomRow = (math.floor(dbottom / cellheight).toInt)
      //       val bottomY = ymin + (bottomRow * cellheight)

      //       val dtop = ymax - sy
      //       val topRow = bottomRow + 1
      //       val topY = bottomY + cellheight

      //       val z =
      //         inverseDeltas *
      //       ( (tile.get(leftCol, topRow) * (dright * dbottom) ) +
      //         (tile.get(rightCol, topRow) * (dleft * dbottom) ) +
      //         (tile.get(leftCol, bottomRow) * (dright * dtop) ) +
      //         (tile.get(rightCol, bottomRow) * (dleft * dtop) ) )

      //       newTile.set(col, row, z)
      //     }
      //   }
      // }

      (newTile, newExtent)
    }
}
