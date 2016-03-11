package geotrellis.raster.merge

import geotrellis.raster._
import geotrellis.raster.resample.{Resample, ResampleMethod}
import geotrellis.vector.Extent
import spire.syntax.cfor._

trait SinglebandTileMergeMethods extends TileMergeMethods[Tile] {
  def merge(other: Tile): Tile = {
    val mutableTile = self.mutable
    Seq(self, other).assertEqualDimensions()
    if (self.cellType.isFloatingPoint) {
      cfor(0)(_ < self.rows, _ + 1) { row =>
        cfor(0)(_ < self.cols, _ + 1) { col =>
          if (isNoData(self.getDouble(col, row))) {
            mutableTile.setDouble(col, row, other.getDouble(col, row))
          }
        }
      }
    } else {
      self.cellType match {
        case BitCellType | UByteConstantNoDataCellType | UShortConstantNoDataCellType =>
          cfor(0)(_ < self.rows, _ + 1) { row =>
            cfor(0)(_ < self.cols, _ + 1) { col =>
              if (self.get(col, row) == 0) {
                mutableTile.setDouble(col, row, other.get(col, row))
              }
            }
          }
        case _ =>
          cfor(0)(_ < self.rows, _ + 1) { row =>
            cfor(0)(_ < self.cols, _ + 1) { col =>
              if (isNoData(self.get(col, row))) {
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
        val mutableTile = self.mutable
        val re = RasterExtent(extent, self.cols, self.rows)
        val GridBounds(colMin, rowMin, colMax, rowMax) = re.gridBoundsFor(sharedExtent)
        val targetCS = CellSize(sharedExtent, colMax, rowMax)

        if (self.cellType.isFloatingPoint) {
          val interpolate = Resample(method, other, otherExtent, targetCS).resampleDouble _
          cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
            cfor(colMin)(_ <= colMax, _ + 1) { col =>
              if (isNoData(self.getDouble(col, row))) {
                val (x, y) = re.gridToMap(col, row)
                mutableTile.setDouble(col, row, interpolate(x, y))
              }
            }
          }
        } else {
          val interpolate = Resample(method, other, otherExtent, targetCS).resample _
          self.cellType match {
            case BitCellType | UByteConstantNoDataCellType | UShortConstantNoDataCellType =>
              cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
                cfor(colMin)(_ <= colMax, _ + 1) { col =>
                  if (self.get(col, row) == 0) {
                    val (x, y) = re.gridToMap(col, row)
                    mutableTile.set(col, row, interpolate(x, y))
                  }
                }
              }
            case _ =>
              cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
                cfor(colMin)(_ <= colMax, _ + 1) { col =>
                  if (isNoData(self.get(col, row))) {
                    val (x, y) = re.gridToMap(col, row)
                    mutableTile.set(col, row, interpolate(x, y))
                  }
                }
              }
          }
        }

        mutableTile
      case _ =>
        self
    }
}
