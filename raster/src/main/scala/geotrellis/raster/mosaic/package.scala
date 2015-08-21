package geotrellis.raster

import geotrellis.vector._
import geotrellis.raster.resample._

import spire.syntax.cfor._

package object mosaic {

  implicit def blankTile = new BlankTile[Tile] {
    def makeFrom(prototype: Tile, cellType: CellType, cols: Int, rows: Int) =
      ArrayTile.empty(cellType, cols, rows)
  }

  implicit def blankMultiBandTile = new BlankTile[MultiBandTile] {
    def makeFrom(prototype: MultiBandTile, cellType: CellType, cols: Int, rows: Int) =
      ArrayMultiBandTile.empty(prototype.bandCount, cellType, cols, rows)
  }

  /** Tile methods used by the mosaicing function to merge tiles. */
  implicit class TileMerger(val tile: Tile) extends MergeTile[Tile] {
    def merge(other: Tile): Tile = {
      val mutableTile = tile.mutable
      Seq(tile, other).assertEqualDimensions
      if (tile.cellType.isFloatingPoint) {
        cfor(0)(_ < tile.rows, _ + 1) { row =>
          cfor(0)(_ < tile.cols, _ + 1) { col =>
            if (isNoData(tile.getDouble(col, row))) {
              mutableTile.setDouble(col, row, other.getDouble(col, row))
            }
          }
        }
      } else {
        cfor(0)(_ < tile.rows, _ + 1) { row =>
          cfor(0)(_ < tile.cols, _ + 1) { col =>
            if (isNoData(tile.get(col, row))) {
              mutableTile.setDouble(col, row, other.get(col, row))
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
          val gb@GridBounds(colMin, rowMin, colMax, rowMax) = re.gridBoundsFor(sharedExtent)
          val otherRe = RasterExtent(otherExtent, other.cols, other.rows)

          if (tile.cellType.isFloatingPoint) {
            val interpolate = Resample(method, other, otherExtent).resampleDouble _
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if (isNoData(tile.getDouble(col, row))) {
                  val (x, y) = re.gridToMap(col, row)
                  mutableTile.setDouble(col, row, interpolate(x, y))
                }
              }
            }
          } else {
            val interpolate = Resample(method, other, otherExtent).resample _
            cfor(rowMin)(_ <= rowMax, _ + 1) { row =>
              cfor(colMin)(_ <= colMax, _ + 1) { col =>
                if (isNoData(tile.get(col, row))) {
                  val (x, y) = re.gridToMap(col, row)
                  mutableTile.set(col, row, interpolate(x, y))
                }
              }
            }

          }

          mutableTile
        case _ =>
          tile
      }
  }

  implicit class MultiBandTileMerger(val tile: MultiBandTile) extends MergeTile[MultiBandTile] {
    def merge(other: MultiBandTile): MultiBandTile = {
      val bands: Seq[Tile] =
        for {
          bandIndex <- 0 until tile.bandCount
        } yield {
          val thisBand = tile.band(bandIndex)
          val thatBand = other.band(bandIndex)
          thisBand.merge(thatBand)
        }

      ArrayMultiBandTile(bands)
    }

    def merge(extent: Extent, otherExtent: Extent, other: MultiBandTile, method: ResampleMethod): MultiBandTile = {
      val bands: Seq[Tile] =
        for {
          bandIndex <- 0 until tile.bandCount
        } yield {
          val thisBand = tile.band(bandIndex)
          val thatBand = other.band(bandIndex)
          thisBand.merge(extent, otherExtent, thatBand, method)
        }

      ArrayMultiBandTile(bands)
    }
  }
}
