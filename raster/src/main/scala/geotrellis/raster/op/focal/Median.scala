package geotrellis.raster.op.focal

import geotrellis.raster._


object MedianCalculation {
  def apply(tile: Tile, n: Neighborhood): FocalCalculation[Tile] =
    n match {
      case Square(ext) => new CellwiseMedianCalc(tile, n, ext)
      case _ => new CursorMedianCalc(tile, n, n.extent)
    }
}

class CursorMedianCalc(r: Tile, n: Neighborhood, extent: Int)
  extends CursorCalculation[Tile](r, n)
  with IntArrayTileResult
  with MedianModeCalculation
{
  initArray(extent)
  init(r)

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

class CellwiseMedianCalc(r: Tile, n: Neighborhood, extent: Int)
  extends CellwiseCalculation[Tile](r, n)
  with IntArrayTileResult
  with MedianModeCalculation
{
  initArray(extent)
  init(r)

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



