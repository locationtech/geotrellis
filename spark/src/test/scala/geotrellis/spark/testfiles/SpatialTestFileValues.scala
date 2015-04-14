package geotrellis.spark.testfiles

import geotrellis.spark._
import geotrellis.raster._

import spire.syntax.cfor._

abstract class TestFileSpatialTiles(tileLayout: TileLayout) {
  final def apply(key: SpatialKey): Tile = {
    val tile = FloatArrayTile.empty(tileLayout.tileCols, tileLayout.tileRows)

    cfor(0)(_ < tileLayout.tileRows, _ + 1) { row =>
      cfor(0)(_ < tileLayout.tileCols, _ + 1) { col =>
        tile.setDouble(col, row, value(key, col, row))
      }
    }

    tile
  }

  def value(key: SpatialKey, col: Int, row: Int): Double
}


class ConstantSpatialTiles(tileLayout: TileLayout, f: Double) extends TestFileSpatialTiles(tileLayout) {
  def value(key: SpatialKey, col: Int, row: Int): Double = f
}

class IncreasingSpatialTiles(tileLayout: TileLayout, gridBounds: GridBounds) extends TestFileSpatialTiles(tileLayout) {
  def value(key: SpatialKey, col: Int, row: Int): Double = {
    val SpatialKey(tileCol, tileRow) = key

    val tc = tileCol - gridBounds.colMin
    val tr = tileRow - gridBounds.rowMin

    val r = (tr * tileLayout.tileRows + row) * (tileLayout.tileCols * gridBounds.width) 
    val c = (tc * tileLayout.tileCols) + col

    r + c
  }
}

class DecreasingSpatialTiles(tileLayout: TileLayout, gridBounds: GridBounds) extends TestFileSpatialTiles(tileLayout) {
  def value(key: SpatialKey, col: Int, row: Int): Double = {
    val SpatialKey(tileCol, tileRow) = key

    val tc = tileCol - gridBounds.colMin
    val tr = tileRow - gridBounds.rowMin

    val r = ((gridBounds.height * tileLayout.tileRows) - (tr * tileLayout.tileRows + row) - 1) * (tileLayout.tileCols * gridBounds.width)
    val c = (tileLayout.tileCols * gridBounds.width - 1) - ((tc * tileLayout.tileCols) + col)

    r + c
  }
}

class EveryOtherSpatialTiles(tileLayout: TileLayout, gridBounds: GridBounds, firstValue: Double, secondValue: Double) extends IncreasingSpatialTiles(tileLayout, gridBounds) {
  override
  def value(key: SpatialKey, col: Int, row: Int): Double =
    if(super.value(key, col, row) % 2 == 0) { firstValue } else { secondValue }
}

class ModSpatialTiles(tileLayout: TileLayout, gridBounds: GridBounds, mod: Int) extends IncreasingSpatialTiles(tileLayout, gridBounds) {
  override
  def value(key: SpatialKey, col: Int, row: Int) = super.value(key, col, row) % mod

}
