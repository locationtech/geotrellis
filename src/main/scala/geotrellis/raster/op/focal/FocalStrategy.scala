package geotrellis.raster.op.focal

import scala.math._

import geotrellis._
import geotrellis.raster._

sealed trait TraversalStrategy
object TraversalStrategy {
  val ZigZag = new TraversalStrategy { }
  val ScanLine = new TraversalStrategy { }
  val SpiralZag = new TraversalStrategy { }
}
import TraversalStrategy._

/**
 * Focal strategy which moves a Cursor across the raster,
 * allowing a calculation to be done on each cell using the Cursor
 * to determine what neighboring cells are inside the focus's
 * neighborhood, what cells have been added since the last move, and
 * what cells have been removed since the last move.
 */
object CursorStrategy {
  def execute(r:Raster,
              n:Neighborhood,
              c:CursorCalculation[_],
              tOpt:Option[TraversalStrategy], 
              neighbors:Seq[Option[Raster]]):Unit = {
    val t = tOpt match {
      case None => ZigZag
      case Some(tStrategy) => tStrategy
    }
    // Get the tile raster
    val (rast,analysisArea) = TileWithNeighbors(r,neighbors)

    val cursor = Cursor(rast,n,analysisArea)
    execute(rast,cursor,c,t,analysisArea)
  }

  def execute(r:Raster,cursor:Cursor,c:CursorCalculation[_],t:TraversalStrategy,analysisArea:GridBounds):Unit = {
    t match {
      case ScanLine => handleScanLine(r, analysisArea, cursor,c)
      case SpiralZag => handleSpiralZag(r,analysisArea,cursor,c)
      case _ => handleZigZag(r,analysisArea,cursor,c)
    }
  }
  
  private def handleSpiralZag(r:Raster,analysisArea:GridBounds,cursor:Cursor,c:CursorCalculation[_]) = {
    var colMax = analysisArea.colMax
    var rowMax = analysisArea.rowMax
    var colMin = analysisArea.colMin
    var rowMin = analysisArea.rowMin

    var col = colMin
    var row = rowMin

    var xdirection = 1
    var ydirection = 1
    var done = false
    var zagTime = false

    cursor.centerOn(col,row)
    
    // Spiral around the raster.
    // Once we get down with dealing with borders,
    // zig zag over inner portion.
    while(!(done || zagTime)) {
      //Move right across top
      while(col < colMax) {
        c.calc(r,cursor)
        cursor.move(Movement.Right)
        col += 1
      }
      // Move down along right edge
      while(row < rowMax) {
        c.calc(r,cursor)
        cursor.move(Movement.Down)
        row += 1
      }
      //Move left across bottom
      while(col > colMin) {
        c.calc(r,cursor)
        cursor.move(Movement.Left)
        col -= 1
      }
      // Move up along left edge
      while(row > rowMin+1) {
        c.calc(r,cursor)
        cursor.move(Movement.Up)
        row -= 1
      }
      c.calc(r,cursor)
      rowMin += 1
      rowMax -= 1
      colMin += 1
      colMax -= 1

      if(rowMin == rowMax || colMin == colMax) { 
        done = true 
      } else {
        cursor.move(Movement.Right)
        col += 1
        if(col - cursor.extent >= 0) {
          zagTime = true
        }
      }
    }

    var direction = 1

    // Now zig zag across interior.
    while(row <= rowMax) {
      c.calc(r,cursor)
      col += direction
      if(col < colMin || colMax < col) {
        direction *= -1
        row += 1
        col += direction
        cursor.move(Movement.Down)
      } else {
        if(direction == 1) { cursor.move(Movement.Right) }
        else { cursor.move(Movement.Left) }
      }
    }
  }

  private def handleZigZag(r:Raster,analysisArea:GridBounds,cursor:Cursor,c:CursorCalculation[_]) = {
    val colMax = analysisArea.colMax
    val rowMax = analysisArea.rowMax
    val colMin = analysisArea.colMin
    val rowMin = analysisArea.rowMin

    var col = colMin
    var row = rowMin

    var direction = 1

    cursor.centerOn(col, row)

    while(row <= rowMax) {
      c.calc(r,cursor)
      col += direction
      if(col < colMin || colMax < col) {
        direction *= -1
        row += 1
        col += direction
        cursor.move(Movement.Down)
      } else {
        if(direction == 1) { cursor.move(Movement.Right) }
        else { cursor.move(Movement.Left) }
      }
    }
  }

  private def handleScanLine(r:Raster,analysisArea:GridBounds,cursor:Cursor,c:CursorCalculation[_]) = {
    val colMax = analysisArea.colMax
    val rowMax = analysisArea.rowMax
    val colMin = analysisArea.colMin
    val rowMin = analysisArea.rowMin

    // set initial state of col and row
    var col = colMin
    var row = rowMin
    cursor.centerOn(col, row)

    while(row <= rowMax) {
      c.calc(r,cursor)
      col += 1
      if(colMax < col) {
        row += 1
        col = colMin
        cursor.centerOn(col,row)
      } else {
        cursor.move(Movement.Right)
      }
    }
  }
}

/**
 * Focal strategy that implements a more strict mechanism that informs the user
 * what cells have been added or removed. This strategy is more performant,
 * but can only be used for Square or Circle neighborhoods.
 */ 
object CellwiseStrategy {
  def execute(r:Raster, 
              n:Square,
              c:CellwiseCalculation[_], 
              tOpt:Option[TraversalStrategy], 
              neighbors:Seq[Option[Raster]]):Unit = {
    val t = tOpt match {
      case None => ScanLine
      case Some(tStrategy) => tStrategy
    }
    val (rast,analysisArea) = TileWithNeighbors(r,neighbors)
    execute(rast,n,c,t,analysisArea)
  }

  def execute(r:Raster,n:Square,calc:CellwiseCalculation[_],t:TraversalStrategy,analysisArea:GridBounds):Unit = {
    t match {
      case _ => handleScanLine(r,n.extent,calc,analysisArea)
    }
  }

  private def handleScanLine(r:Raster,n:Int, calc:CellwiseCalculation[_], analysisArea:GridBounds) = {
    val rowMin = analysisArea.rowMin
    val colMin = analysisArea.colMin
    val rowMax = analysisArea.rowMax
    val rowBorderMax = r.rows - 1
    val colMax = analysisArea.colMax
    val colBorderMax = r.cols - 1

    val analysisOffsetCols = analysisArea.colMin
    val analysisOffsetRows = analysisArea.rowMin

    var focusRow = rowMin
    while (focusRow <= rowMax) {
      val curRowMin = max(0, focusRow - n)
      val curRowMax = min(rowBorderMax, focusRow + n )

      calc.reset()
      val curColMax = min(colBorderMax, colMin + n)
      val curColMin = max(0, colMin - n)
      var curRow = curRowMin
      while (curRow <= curRowMax) {
        var curCol = curColMin
        while (curCol <= curColMax) {
          calc.add(r, curCol, curRow)
          curCol += 1
        }
        curRow += 1
      }

      // offset output col & row to analysis area coordinates
      calc.setValue(0, focusRow - rowMin) 

      var focusCol = colMin + 1
      while (focusCol <= colMax) {
        // Remove the western most column that is no longer part of the neighborhood
        val oldWestCol = focusCol - n - 1
        if (oldWestCol >= 0) {
          var yy = curRowMin
          while (yy <= curRowMax) {
            calc.remove(r, oldWestCol, yy)
            yy += 1
          }
        }

        // Add the eastern most column that is now part of the neighborhood
        val newEastCol = focusCol + n
        if (newEastCol <= colBorderMax) {
            var yy = curRowMin
            while (yy <= curRowMax) {
              calc.add(r, newEastCol, yy)
              yy += 1
            }
          }

        // offset output col & row to analysis area coordinates
        calc.setValue(focusCol - colMin, focusRow - rowMin)
        focusCol += 1
      }
      focusRow += 1
    }
  }
}
