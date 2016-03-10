package geotrellis.spark.testfiles

import geotrellis.spark._
import geotrellis.raster._

import spire.syntax.cfor._

abstract class TestFileGridTimeKeyTiles(tileLayout: TileLayout) {
  final def apply(key: GridTimeKey, timeIndex: Int): Tile = {
    val tile = FloatArrayTile.empty(tileLayout.tileCols, tileLayout.tileRows)

    cfor(0)(_ < tileLayout.tileRows, _ + 1) { row =>
      cfor(0)(_ < tileLayout.tileCols, _ + 1) { col =>
        tile.setDouble(col, row, value(key, timeIndex, col, row))
      }
    }

    tile
  }

  def value(key: GridTimeKey, timeIndex: Int, col: Int, row: Int): Double
}


class ConstantGridTimeKeyTestTiles(tileLayout: TileLayout, v: Double) extends TestFileGridTimeKeyTiles(tileLayout) {
  def value(key: GridTimeKey, timeIndex: Int, col: Int, row: Int): Double = v
}

/** Coordinates are CCC,RRR.TTT where C = column, R = row, T = time (year in 2010 + T).
  * So 34,025.004 would represent col 34, row 25, year 2014
  */
class CoordinateGridTimeKeyTestTiles(tileLayout: TileLayout) extends TestFileGridTimeKeyTiles(tileLayout) {
  def value(key: GridTimeKey, timeIndex: Int, col: Int, row: Int): Double= {
    val GridTimeKey(layoutCol, layoutRow, _) = key
    (layoutCol * 1000.0) + layoutRow + (timeIndex / 1000.0)
  }
}
