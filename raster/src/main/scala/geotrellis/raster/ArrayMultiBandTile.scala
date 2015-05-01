package geotrellis.raster

import geotrellis.raster.interpolation._
import geotrellis.vector.Extent
import geotrellis.raster.op.stats._

import spire.syntax.cfor._

import java.util.Locale

import math.BigDecimal

class ArrayMultiBandTile(bands: Array[ArrayTile]) {
  val bandCount = bands.size

  assert(bandCount > 0, "Band count must be greater than 0")

  val cellType = bands(0).cellType
  val cols: Int = bands(0).cols
  val rows: Int = bands(0).rows

  // Check all bands for consistency.
  // I wish this wasn't a run time check.
  cfor(0)(_ < bandCount, _ + 1) { i =>
    assert(bands(i).cellType == cellType, s"Band $i cell type does not match, ${bands(i).cellType} != $cellType")
    assert(bands(i).cols == cols, s"Band $i cols does not match, ${bands(i).cols} != $cols")
    assert(bands(i).rows == rows, s"Band $i rows does not match, ${bands(i).rows} != $rows")
  }

  lazy val dimensions: (Int, Int) = (cols, rows)
  lazy val size = cols * rows

  def band(bandIndex: Int): Tile = {
    if(bandIndex >= bandCount) { throw new IllegalArgumentException(s"Band $bandIndex does not exist") }
    bands(bandIndex)
  }

}
