package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._


object Median {
  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds], target: TargetCell = TargetCell.All): FocalCalculation[Tile] = {
    n match {
      case Square(ext) => new CellwiseMedianCalc(tile, n, bounds, ext, target)
      case _ => new CursorMedianCalc(tile, n, bounds, n.extent, target)
    }
  }

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds], target: TargetCell = TargetCell.All): Tile =
    calculation(tile, n, bounds, target).execute()
}

class CursorMedianCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds], extent: Int, target: TargetCell)
  extends CursorCalculation[Tile](r, n, bounds, target)
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

class CellwiseMedianCalc(r: Tile, n: Neighborhood, bounds: Option[GridBounds], extent: Int, target: TargetCell)
  extends CellwiseCalculation[Tile](r, n, bounds, target)
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

  def setValue(x: Int, y: Int) = { resultTile.set(x, y, median) }
}
