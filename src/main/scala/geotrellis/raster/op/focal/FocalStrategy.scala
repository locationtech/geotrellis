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
  def execute(r:Raster,cursor:Cursor,c:CursorCalculation[_],tOpt:Option[TraversalStrategy],reOpt:Option[RasterExtent]):Unit =  {
    tOpt match {
      case Some(t) => execute(r,cursor,c,t,reOpt)
      case None    => execute(r,cursor,c,ZigZag,reOpt) 
    } 
  }

  def execute(r:Raster,cursor:Cursor,c:CursorCalculation[_],t:TraversalStrategy,reOpt:Option[RasterExtent]=None):Unit = {
    val analysisArea = FocalOperation.calculateAnalysisArea(r, reOpt)
    t match {
      case ScanLine => handleScanLine(r, analysisArea, cursor,c)
      case SpiralZag => handleSpiralZag(r,analysisArea,cursor,c)
      case _ => handleZigZag(r,analysisArea,cursor,c)
    }
  }
  
  private def handleSpiralZag(r:Raster,analysisArea:AnalysisArea,cursor:Cursor,c:CursorCalculation[_]) = {
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

  private def handleZigZag(r:Raster,analysisArea:AnalysisArea,cursor:Cursor,c:CursorCalculation[_]) = {
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

  private def handleScanLine(r:Raster,analysisArea:AnalysisArea,cursor:Cursor,c:CursorCalculation[_]) = {
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
  def execute(r:Raster,n:Square,calc:CellwiseCalculation[_],tOpt:Option[TraversalStrategy], reOpt:Option[RasterExtent]):Unit = tOpt match {
      case None => execute(r,n,calc,ScanLine,reOpt)
      case Some(t) => execute(r,n,calc,t,reOpt)
  }

 def execute(r:Raster,n:Square,calc:CellwiseCalculation[_],t:TraversalStrategy=ScanLine,reOpt:Option[RasterExtent]=None):Unit = {
    val analysisArea = FocalOperation.calculateAnalysisArea(r, reOpt)
    t match {
      case _ => handleScanLine(r,n.extent,calc,analysisArea)
    }
  }

  private def handleScanLine(r:Raster,n:Int, calc:CellwiseCalculation[_], analysisArea:AnalysisArea) = {
    val rowMin = analysisArea.rowMin
    val colMin = analysisArea.colMin
    val rowMax = analysisArea.rowMax
    val colMax = analysisArea.colMax

    val analysisOffsetCols = analysisArea.colMin
    val analysisOffsetRows = analysisArea.rowMin

    var row = rowMin
    while (row <= rowMax) {
      val curRowMin = max(rowMin, row - n) // was yy1
      val curRowMax = min(rowMax, row + n ) // was yy2

      calc.reset()
      val xx2 = min(colMax, colMin + n)
      var yy = curRowMin
      while (yy <= curRowMax) {
        var xx = colMin
        while (xx <= xx2) {
          calc.add(r, xx, yy)
          xx += 1
        }
        yy += 1
      }

      // offset output col & row to analysis area coordinates
      calc.setValue(0, row - rowMin) 

      var col = colMin + 1
      while (col <= colMax) {
        val xx1 = col - n - 1
        if (xx1 >= 0) {
          var yy = curRowMin
          while (yy <= curRowMax) {
            calc.remove(r, xx1, yy)
            yy += 1
          }
        }

        val xx2 = col + n
        if (xx2 <= colMax) {
            var yy = curRowMin
            while (yy <= curRowMax) {
              calc.add(r, xx2, yy)
              yy += 1
            }
          }

        // offset output col & row to analysis area coordinates
        calc.setValue(col - colMin, row - rowMin)
        col += 1
      }
      row += 1
    }
  }
}
