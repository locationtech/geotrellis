package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.TargetCell.TargetCell


object Median {
  def calculation(tile: Tile, n: Neighborhood, target: TargetCell, bounds: Option[GridBounds]): FocalCalculation[Tile] = {
    n match {
      case Square(ext) => new CellwiseMedianCalc(tile, n, target, bounds, ext)
      case _ => new CursorMedianCalc(tile, n, target, bounds, n.extent)
    }
  }

  def apply(tile: Tile, n: Neighborhood, target: TargetCell, bounds: Option[GridBounds]): Tile =
    calculation(tile, n, target, bounds).execute()
}

class CursorMedianCalc(tile: Tile, n: Neighborhood, target: TargetCell, bounds: Option[GridBounds], extent: Int)
  extends CursorCalculation[Tile](tile, n, target, bounds)
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

class CellwiseMedianCalc(tile: Tile, n: Neighborhood, target: TargetCell, bounds: Option[GridBounds], extent: Int)
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

  def setValue(x: Int, y: Int, focusCol: Int, focusRow: Int) =
    setValidResult(x, y, focusCol, focusRow, median)
}



