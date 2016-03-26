package geotrellis.raster.merge

import geotrellis.raster._
import geotrellis.raster.resample.{Resample, ResampleMethod}
import geotrellis.vector.Extent

import spire.syntax.cfor._

/**
  * Trait containing extension methods for doing merge operations on
  * single-band [[Tile]]s.
  */
trait SinglebandTileMergeMethods extends TileMergeMethods[Tile] {
  /** Merges this tile with another tile.
    *
    * This method will replace the values of these cells with the
    * values of the other tile's corresponding cells, if the source
    * cell is of the transparent value.  The transparent value is
    * determined by the tile's cell type; if the cell type has a
    * NoData value, then that is considered the transparent value.  If
    * there is no NoData value associated with the cell type, then a 0
    * value is considered the transparent value. If this is not the
    * desired effect, the caller is required to change the cell type
    * before using this method to an appropriate cell type that has
    * the desired NoData value.
    *
    * @note                         This method requires that the dimensions be the same between the tiles, and assumes
    *                               equal extents.
    */
  def merge(other: Tile): Tile = {
    val mutableTile = self.mutable
    Seq(self, other).assertEqualDimensions()
    self.cellType match {
      case BitCellType =>
        cfor(0)(_ < self.rows, _ + 1) { row =>
          cfor(0)(_ < self.cols, _ + 1) { col =>
            if (other.get(col, row) == 1) {
              mutableTile.set(col, row, 1)
            }
          }
        }
      case ByteCellType | UByteCellType | ShortCellType | UShortCellType | IntCellType  =>
        // Assume 0 as the transparent value
        cfor(0)(_ < self.rows, _ + 1) { row =>
          cfor(0)(_ < self.cols, _ + 1) { col =>
            if (self.get(col, row) == 0) {
              mutableTile.set(col, row, other.get(col, row))
            }
          }
        }
      case FloatCellType | DoubleCellType =>
        // Assume 0.0 as the transparent value
        cfor(0)(_ < self.rows, _ + 1) { row =>
          cfor(0)(_ < self.cols, _ + 1) { col =>
            if (self.getDouble(col, row) == 0.0) {
              mutableTile.setDouble(col, row, other.getDouble(col, row))
            }
          }
        }
      case x if x.isFloatingPoint =>
        cfor(0)(_ < self.rows, _ + 1) { row =>
          cfor(0)(_ < self.cols, _ + 1) { col =>
            if (isNoData(self.getDouble(col, row))) {
              mutableTile.setDouble(col, row, other.getDouble(col, row))
            }
          }
        }
      case _ =>
        cfor(0)(_ < self.rows, _ + 1) { row =>
          cfor(0)(_ < self.cols, _ + 1) { col =>
            if (isNoData(self.get(col, row))) {
              mutableTile.set(col, row, other.get(col, row))
            }
          }
        }
    }

    mutableTile
  }

  /** Merges this tile with another tile, given the extents both tiles.
    *
    * This method will replace the values of these cells with a
    * resampled value taken from the tile's cells, if the source cell
    * is of the transparent value.  The transparent value is
    * determined by the tile's cell type; if the cell type has a
    * NoData value, then that is considered the transparent value.  If
    * there is no NoData value associated with the cell type, then a 0
    * value is considered the transparent value. If this is not the
    * desired effect, the caller is required to change the cell type
    * before using this method to an appropriate cell type that has
    * the desired NoData value.
    */
  def merge(extent: Extent, otherExtent: Extent, other: Tile, method: ResampleMethod): Tile =
    otherExtent & extent match {
      case Some(sharedExtent) =>
        val mutableTile = self.mutable
        val re = RasterExtent(extent, self.cols, self.rows)
        val GridBounds(colMin, rowMin, colMax, rowMax) = re.gridBoundsFor(sharedExtent)
        val targetCS = CellSize(sharedExtent, colMax, rowMax)

        self.cellType match {
          case BitCellType | ByteCellType | UByteCellType | ShortCellType | UShortCellType | IntCellType  =>
            val interpolate = Resample(method, other, otherExtent, targetCS).resample _
            // Assume 0 as the transparent value
            cfor(0)(_ < self.rows, _ + 1) { row =>
              cfor(0)(_ < self.cols, _ + 1) { col =>
                if (self.get(col, row) == 0) {
                  val (x, y) = re.gridToMap(col, row)
                  mutableTile.set(col, row, interpolate(x, y))
                }
              }
            }
          case FloatCellType | DoubleCellType =>
            val interpolate = Resample(method, other, otherExtent, targetCS).resampleDouble _
            // Assume 0.0 as the transparent value
            cfor(0)(_ < self.rows, _ + 1) { row =>
              cfor(0)(_ < self.cols, _ + 1) { col =>
                if (self.getDouble(col, row) == 0.0) {
                  val (x, y) = re.gridToMap(col, row)
                  mutableTile.setDouble(col, row, interpolate(x, y))
                }
              }
            }
          case x if x.isFloatingPoint =>
            val interpolate = Resample(method, other, otherExtent, targetCS).resampleDouble _
            cfor(0)(_ < self.rows, _ + 1) { row =>
              cfor(0)(_ < self.cols, _ + 1) { col =>
                if (isNoData(self.getDouble(col, row))) {
                  val (x, y) = re.gridToMap(col, row)
                  mutableTile.setDouble(col, row, interpolate(x, y))
                }
              }
            }
          case _ =>
            val interpolate = Resample(method, other, otherExtent, targetCS).resample _
            cfor(0)(_ < self.rows, _ + 1) { row =>
              cfor(0)(_ < self.cols, _ + 1) { col =>
                if (isNoData(self.get(col, row))) {
                  val (x, y) = re.gridToMap(col, row)
                  mutableTile.set(col, row, interpolate(x, y))
                }
              }
            }
        }

        mutableTile
      case _ =>
        self
    }
}
