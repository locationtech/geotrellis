package geotrellis.raster

import geotrellis.vector._
import geotrellis.raster.resample._

import spire.syntax.cfor._

package object mosaic {
  /** Tile methods used by the mosaicing function to merge tiles. */
  implicit class TileMerger(val tile: Tile) {
    def merge(other: Tile): Tile = {
      val mutableTile = tile.mutable
      Seq(tile, other).assertEqualDimensions
      if(tile.cellType.isFloatingPoint) {
        cfor(0)(_ < tile.rows, _ + 1) { row =>
          cfor(0)(_ < tile.cols, _ + 1) { col =>
            if(isNoData(tile.getDouble(col, row))) {
              mutableTile.setDouble(col, row, other.getDouble(col, row))
            }
          }
        }
      } else {
        cfor(0)(_ < tile.rows, _ + 1) { row =>
          cfor(0)(_ < tile.cols, _ + 1) { col =>
            if(isNoData(tile.get(col, row))) {
              mutableTile.setDouble(col, row, other.get(col, row))
            }
          }
        }
      }

      mutableTile
    }

    def merge(extent: Extent, otherExtent: Extent, other: Tile): Tile =
      merge(extent, otherExtent, other, NearestNeighbor)

    def merge(extent: Extent, otherExtent: Extent, other: Tile, method: ResampleMethod): Tile =
      otherExtent & extent match {
        case Some(sharedExtent) =>
          val mutableTile = tile.mutable
          val re = RasterExtent(extent, tile.cols, tile.rows)
          val gb @ GridBounds(colMin, rowMin, colMax, rowMax) = re.gridBoundsFor(sharedExtent)
          val otherRe = RasterExtent(otherExtent, other.cols, other.rows)

          if(tile.cellType.isFloatingPoint) {
            val resampleF = Resample(method, other, otherExtent).resampleDouble _
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if(isNoData(tile.getDouble(col, row))) {
                  val (x, y) = re.gridToMap(col, row)
                  mutableTile.setDouble(col, row, resampleF(x, y))
                }
              }
            }
          } else {
            val resampleF = Resample(method, other, otherExtent).resample _
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if(isNoData(tile.get(col, row))) {
                  val (x, y) = re.gridToMap(col, row)
                  mutableTile.set(col, row, resampleF(x, y))
                }
              }
            }

          }

          mutableTile
        case _ =>
          tile
      }
  }
}
