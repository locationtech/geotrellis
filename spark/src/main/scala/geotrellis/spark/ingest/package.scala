package geotrellis.spark

import geotrellis.raster._
import geotrellis.vector._

import spire.syntax.cfor._

package object ingest {
  /** Tile methods used by the mosaicing function to merge tiles. */
  implicit class TileMerger(val tile: MutableArrayTile) {
    def merge(other: Tile): MutableArrayTile = {
      Seq(tile, other).assertEqualDimensions
      if(tile.cellType.isFloatingPoint) {
        cfor(0)(_ < tile.rows, _ + 1) { row =>
          cfor(0)(_ < tile.cols, _ + 1) { col =>
            if(isNoData(tile.getDouble(col, row))) {
              tile.setDouble(col, row, other.getDouble(col, row))
            }
          }
        }
      } else {
        cfor(0)(_ < tile.rows, _ + 1) { row =>
          cfor(0)(_ < tile.cols, _ + 1) { col =>
            if(isNoData(tile.get(col, row))) {
              tile.setDouble(col, row, other.get(col, row))
            }
          }
        }
      }

      tile
    }

    def merge(extent: Extent, otherExtent: Extent, other: Tile): MutableArrayTile =
      otherExtent & extent match {
        case PolygonResult(sharedExtent) =>
          val re = RasterExtent(extent, tile.cols, tile.rows)
          val GridBounds(colMin, colMax, rowMin, rowMax) = re.gridBoundsFor(sharedExtent)
          val otherRe = RasterExtent(otherExtent, other.cols, other.rows)

          def thisToOther(col: Int, row: Int): (Int, Int) = {
            val (x, y) = re.gridToMap(col, row)
            otherRe.mapToGrid(x, y)
          }

          if(tile.cellType.isFloatingPoint) {
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if(isNoData(tile.getDouble(col, row))) {
                  val (otherCol, otherRow) = thisToOther(col, row)
                  if(otherCol > 0 && otherCol < other.cols &&
                    otherRow > 0 && otherRow < other.rows)
                    tile.setDouble(col, row, other.getDouble(otherCol, otherRow))
                }
              }
            }
          } else {
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if(isNoData(tile.get(col, row))) {
                  val (otherCol, otherRow) = thisToOther(col, row)
                  if(otherCol > 0 && otherCol < other.cols &&
                    otherRow > 0 && otherRow < other.rows)
                    tile.set(col, row, other.get(otherCol, otherRow))
                }
              }
            }

          }

          tile
        case _ =>
          tile
      }
  }
}
