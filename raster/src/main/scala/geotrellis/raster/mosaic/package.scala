package geotrellis.raster

import geotrellis.vector._
import geotrellis.raster.interpolation._

import spire.syntax.cfor._

package object mosaic {
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
      merge(extent, otherExtent, other, NearestNeighbor)

    def merge(extent: Extent, otherExtent: Extent, other: Tile, method: InterpolationMethod): MutableArrayTile =
      otherExtent & extent match {
        case PolygonResult(sharedExtent) =>
          val re = RasterExtent(extent, tile.cols, tile.rows)
          val GridBounds(colMin, rowMin, colMax, rowMax) = re.gridBoundsFor(sharedExtent)
          val otherRe = RasterExtent(otherExtent, other.cols, other.rows)

          if(tile.cellType.isFloatingPoint) {
            val interpolate = Interpolation(method, other, otherExtent).interpolateDouble _
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if(isNoData(tile.getDouble(col, row))) {
                  val (x, y) = re.gridToMap(col, row)
                  tile.setDouble(col, row, interpolate(x, y))
                }
              }
            }
          } else {
            val interpolate = Interpolation(method, other, otherExtent).interpolate _
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if(isNoData(tile.get(col, row))) {
                  val (x, y) = re.gridToMap(col, row)
                  tile.set(col, row, interpolate(x, y))
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
