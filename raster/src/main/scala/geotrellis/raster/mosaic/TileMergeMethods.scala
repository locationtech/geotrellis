package geotrellis.raster.mosaic

import geotrellis.raster._
import geotrellis.raster.resample.{Resample, ResampleMethod}
import geotrellis.vector.Extent
import spire.syntax.cfor._

trait TileMergeMethods extends MergeMethods[Tile] {
  def merge(other: Tile): Tile = {
    val mutableTile = tile.mutable
    Seq(tile, other).assertEqualDimensions()
    if (tile.cellType.isFloatingPoint) {
      cfor(0)(_ < tile.rows, _ + 1) { row =>
        cfor(0)(_ < tile.cols, _ + 1) { col =>
          if (isNoData(tile.getDouble(col, row))) {
            mutableTile.setDouble(col, row, other.getDouble(col, row))
          }
        }
      }
    } else {
      tile.cellType match {
        case TypeBit | TypeUByte | TypeUShort =>
          cfor(0)(_ < tile.rows, _ + 1) { row =>
            cfor(0)(_ < tile.cols, _ + 1) { col =>
              if (tile.get(col, row) == 0) {
                mutableTile.setDouble(col, row, other.get(col, row))
              }
            }
          }
        case _ =>
          cfor(0)(_ < tile.rows, _ + 1) { row =>
            cfor(0)(_ < tile.cols, _ + 1) { col =>
              if (isNoData(tile.get(col, row))) {
                mutableTile.setDouble(col, row, other.get(col, row))
              }
            }
          }
      }
    }

    mutableTile
  }

  def merge(extent: Extent, otherExtent: Extent, other: Tile, method: ResampleMethod): Tile =
    otherExtent & extent match {
      case Some(sharedExtent) =>
        val mutableTile = tile.mutable
        val re = RasterExtent(extent, tile.cols, tile.rows)
        val GridBounds(colMin, rowMin, colMax, rowMax) = re.gridBoundsFor(sharedExtent)
        val targetCS = CellSize(sharedExtent, colMax, rowMax)

        if (tile.cellType.isFloatingPoint) {
          val interpolate = Resample(method, other, otherExtent, targetCS).resampleDouble _
          cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
            cfor(colMin)(_ <= colMax, _ + 1) { col =>
              if (isNoData(tile.getDouble(col, row))) {
                val (x, y) = re.gridToMap(col, row)
                mutableTile.setDouble(col, row, interpolate(x, y))
              }
            }
          }
        } else {
          val interpolate = Resample(method, other, otherExtent, targetCS).resample _
          tile.cellType match {
            case TypeBit | TypeUByte | TypeUShort =>
              cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
                cfor(colMin)(_ <= colMax, _ + 1) { col =>
                  if (tile.get(col, row) == 0) {
                    val (x, y) = re.gridToMap(col, row)
                    mutableTile.set(col, row, interpolate(x, y))
                  }
                }
              }
            case _ =>
              cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
                cfor(colMin)(_ <= colMax, _ + 1) { col =>
                  if (isNoData(tile.get(col, row))) {
                    val (x, y) = re.gridToMap(col, row)
                    mutableTile.set(col, row, interpolate(x, y))
                  }
                }
              }
          }
        }

        mutableTile
      case _ =>
        tile
    }
}
