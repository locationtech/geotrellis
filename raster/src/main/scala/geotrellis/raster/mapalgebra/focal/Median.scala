package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.FocalTarget.FocalTarget


object Median {
  def calculation(tile: Tile, n: Neighborhood, target: FocalTarget, bounds: Option[GridBounds]): FocalCalculation[Tile] = {
    n match {
      case Square(ext) => new CellwiseMedianCalc(tile, n, target, bounds, ext)
      case _ => new CursorMedianCalc(tile, n, bounds, n.extent)
    }
  }

  def apply(tile: Tile, n: Neighborhood, target: FocalTarget, bounds: Option[GridBounds]): Tile =
    calculation(tile, n, target, bounds).execute()
}

class CursorMedianCalc(tile: Tile, n: Neighborhood, bounds: Option[GridBounds], extent: Int)
  extends CursorCalculation[Tile](tile, n, FocalTarget.Tmp, bounds)
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
    resultTile.set(cursor.col, cursor.row, median)
  }
}

class CellwiseMedianCalc(tile: Tile, n: Neighborhood, target: FocalTarget, bounds: Option[GridBounds], extent: Int)
  extends CellwiseCalculation[Tile](tile, n, target, bounds)
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

  def setValue(x: Int, y: Int) =
    resultTile.set(x, y, median)

  def copy(focusCol: Int, focusRow: Int, x: Int, y: Int) =
    resultTile.set(x, y, tile.get(focusCol, focusRow))
}



