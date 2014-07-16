package geotrellis.raster.op.focal

import geotrellis.raster._


object MedianCalculation {
  def apply(tile: Tile, n: Neighborhood): FocalCalculation[Tile] with Initialization =
    n match {
      case Square(ext) => new CellwiseMedianCalc(ext)
      case _ => new CursorMedianCalc(n.extent)
    }
}

class CursorMedianCalc(extent: Int)
  extends CursorCalculation[Tile]
  with IntArrayTileResult
  with MedianModeCalculation
{
  initArray(extent)

  def calc(r: Tile, cursor: Cursor) = {
    cursor.removedCells.foreach { (x, y) =>
      val v = r.get(x, y)
      if(isData(v)) {
        removeValue(v)
      }
    }
    cursor.addedCells.foreach { (x, y) =>
      val v = r.get(x, y)
      if(isData(v)) addValueOrdered(v)
    }
    tile.set(cursor.col, cursor.row, median)
  }
}

class CellwiseMedianCalc(extent: Int)
  extends CellwiseCalculation[Tile]
  with IntArrayTileResult
  with MedianModeCalculation
{
  initArray(extent)

  def add(r: Tile, x: Int, y: Int) = {
    val v = r.get(x, y)
    if (isData(v)) {
      addValueOrdered(v)
    }
  }

  def remove(r: Tile, x: Int, y: Int) = {
    val v = r.get(x, y)
    if (isData(v)) {
      removeValue(v)
    }
  }

  def setValue(x: Int, y: Int) = { tile.setDouble(x, y, median) }
}



