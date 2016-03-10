package geotrellis.spark.testfiles

import geotrellis.spark._
import geotrellis.raster._

import spire.syntax.cfor._

abstract class TestFileGridKeyTiles(tileLayout: TileLayout) {
  final def apply(key: GridKey): Tile = {
    val tile = FloatArrayTile.empty(tileLayout.tileCols, tileLayout.tileRows)

    cfor(0)(_ < tileLayout.tileRows, _ + 1) { row =>
      cfor(0)(_ < tileLayout.tileCols, _ + 1) { col =>
        tile.setDouble(col, row, value(key, col, row))
      }
    }

    tile
  }

  def value(key: GridKey, col: Int, row: Int): Double
}


class ConstantGridKeyTiles(tileLayout: TileLayout, f: Double) extends TestFileGridKeyTiles(tileLayout) {
  def value(key: GridKey, col: Int, row: Int): Double = f
}

class IncreasingGridKeyTiles(tileLayout: TileLayout, gridBounds: GridBounds) extends TestFileGridKeyTiles(tileLayout) {
  def value(key: GridKey, col: Int, row: Int): Double = {
    val GridKey(tileCol, tileRow) = key

    val tc = tileCol - gridBounds.colMin
    val tr = tileRow - gridBounds.rowMin

    val r = (tr * tileLayout.tileRows + row) * (tileLayout.tileCols * gridBounds.width) 
    val c = (tc * tileLayout.tileCols) + col

    r + c
  }
}

class DecreasingGridKeyTiles(tileLayout: TileLayout, gridBounds: GridBounds) extends TestFileGridKeyTiles(tileLayout) {
  def value(key: GridKey, col: Int, row: Int): Double = {
    val GridKey(tileCol, tileRow) = key

    val tc = tileCol - gridBounds.colMin
    val tr = tileRow - gridBounds.rowMin

    val r = ((gridBounds.height * tileLayout.tileRows) - (tr * tileLayout.tileRows + row) - 1) * (tileLayout.tileCols * gridBounds.width)
    val c = (tileLayout.tileCols * gridBounds.width - 1) - ((tc * tileLayout.tileCols) + col)

    r + c
  }
}

class EveryOtherGridKeyTiles(tileLayout: TileLayout, gridBounds: GridBounds, firstValue: Double, secondValue: Double) extends IncreasingGridKeyTiles(tileLayout, gridBounds) {
  override
  def value(key: GridKey, col: Int, row: Int): Double =
    if(super.value(key, col, row) % 2 == 0) { firstValue } else { secondValue }
}

class ModGridKeyTiles(tileLayout: TileLayout, gridBounds: GridBounds, mod: Int) extends IncreasingGridKeyTiles(tileLayout, gridBounds) {
  override
  def value(key: GridKey, col: Int, row: Int) = super.value(key, col, row) % mod

}
