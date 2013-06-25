package geotrellis.raster.op

import geotrellis._
import geotrellis.feature._
import geotrellis.raster.op.focal.{RegionGroup,RegionGroupResult}
import geotrellis.raster.CroppedRaster

import com.vividsolutions.jts.geom

import scala.collection.mutable

import spire.syntax._

object ToVector {
  def apply(r:Op[Raster]):Op[List[Polygon[Int]]] = {
    RegionGroup(r).flatMap { rgr =>
      val p = new Polygonizer(rgr.raster)
      (for(v <- rgr.regionMap.keys) yield {
        p.getPolygon(v, rgr.regionMap(v))
      }).toList
    }
  }
}

/* Rules:
 * Start with top left. Mark point on Top Left corner
 * Check adjacent cells in counter clockwise starting from left:
 *   - left, down, right, up
 * 
 *  On move, if the previous move is in the same direction, make no marks.
 *  If the previous move and this move make a right hand turn,
 *  a mark is made on the border of this cell and the previous cell
 *  at the corner of the turn.
 *  If they make a left hand turn, a mark is made on the edge opposite of
 *  the border of this cell and the previous cell at the corner of the turn.
 *  If the previous move was in the opposite direction, mark the two
 *  corners of the edge opposite of the border of this cell and the
 *  previous cell.
 * 
 *  Marks are signified by TL,TR,BL,BR based on previous cell.
 * 
 *  TL = Top Left  TR = Top Right  BL = Bottom Left   BR = Bottom Right 
 *  D = Down  U = Up  L = Left  R = Right
 *  PM = Previous Move   LHT = Left Hand Turn   RHT = Right Hand Turn
 *                        RT = Reverse Turn
 * 
 *  Move   PM RHT  Mark   PM LHT  Mark    PM RT  Mark
 * ------ -------------- --------------- -------------
 *  D        R      BL      L      TL       U    TL,TR 
 *  R        U      BR      D      BL       L    TL,BL
 *  U        L      TR      R      BR       D    BL,BR
 *  L        D      TL      U      TR       R    BR,TR
 * 
 */
class Polygonizer(val r:Raster) {
  val re = r.rasterExtent
  val cols = re.cols
  val rows = re.rows
  val halfCellWidth = re.cellwidth / 2.0
  val halfCellHeight = re.cellheight / 2.0

  // Directions
  val NOTFOUND = -1
  val LEFT = 0
  val DOWN = 1
  val RIGHT = 2
  val UP = 3

  // Marks
  val TOPLEFT = 0
  val BOTTOMLEFT = 1
  val BOTTOMRIGHT = 2
  val TOPRIGHT = 3

  def sd(d:Int) = 
    d match {
      case NOTFOUND => "NOTFOUND"
      case LEFT => "LEFT"
      case DOWN => "DOWN"
      case RIGHT => "RIGHT"
      case UP => "UP"
      case _ => "BAD"
    }

  def sm(d:Int) = 
    d match {
      case TOPLEFT => "TOPLEFT"
      case BOTTOMLEFT => "BOTTOMLEFT"
      case BOTTOMRIGHT => "BOTTOMRIGHT"
      case TOPRIGHT => "TOPRIGHT"
      case _ => "BAD"
    }

  def mark(col:Int,row:Int,m:Int):geom.Coordinate = {
    val mapX = re.gridColToMap(col)
    val mapY = re.gridRowToMap(row)
    if(m == TOPLEFT) {
      new geom.Coordinate(mapX - halfCellWidth, mapY + halfCellHeight)
    } else if(m == BOTTOMLEFT) {
      new geom.Coordinate(mapX - halfCellWidth, mapY - halfCellHeight)
    } else if(m == BOTTOMRIGHT) {
      new geom.Coordinate(mapX + halfCellWidth, mapY - halfCellHeight)
    } else if(m == TOPRIGHT) {
      new geom.Coordinate(mapX + halfCellWidth, mapY + halfCellHeight)
    } else {
      sys.error("Bad Mark Integer")
    }
  }

  /*  Move   PM RHT  Mark   PM LHT  Mark    PM RT  Mark
   * ------ -------------- --------------- -------------
   *  D        R      BL      L      TL       U    TL,TR 
   *  R        U      BR      D      BL       L    BL,TL
   *  U        L      TR      R      BR       D    BR,BL
   *  L        D      TL      U      TR       R    TR,BR
   */
  def makeMarks(points:mutable.ArrayBuffer[geom.Coordinate],
                col:Int,row:Int,d:Int,pd:Int) = {
    if(d == DOWN) {
      if(pd != DOWN) {
        if(pd == RIGHT) {                //RHT
          points += mark(col,row-1,BOTTOMLEFT)
        } else if(pd == LEFT) {          //LHT
          points += mark(col,row-1,TOPLEFT)
        } else {                         //RT
          points += mark(col,row-1,TOPLEFT)
          points += mark(col,row-1,TOPRIGHT)
        }
      }
    } else if(d == RIGHT) {
      if(pd != RIGHT) {
        if(pd == UP) {                   //RHT
          points += mark(col-1,row,BOTTOMRIGHT)
        } else if(pd == DOWN) {          //LHT
          points += mark(col-1,row,BOTTOMLEFT)
        } else {                         //RT
          points += mark(col-1,row,BOTTOMLEFT)
          points += mark(col-1,row,TOPLEFT)
        }
      }
    } else if(d == UP) {
      if(pd != UP) {
        if(pd == LEFT) {                 //RHT
          points += mark(col,row+1,TOPRIGHT)
        } else if(pd == RIGHT) {         //LHT
          points += mark(col,row+1,BOTTOMRIGHT)
        } else {                         //RT
          points += mark(col,row+1,BOTTOMRIGHT)
          points += mark(col,row+1,BOTTOMLEFT)
        }
      }
    } else if(d == LEFT) {
      if(pd != LEFT) {
        if(pd == DOWN) {                 //RHT
          points += mark(col+1,row,TOPLEFT)
        } else if(pd == UP) {            //LHT
          points += mark(col+1,row,TOPRIGHT)
        } else {                         //RT
          points += mark(col+1,row,TOPRIGHT)
          points += mark(col+1,row,BOTTOMRIGHT)
        }
      }
    } else { sys.error(s"Unknown direction $d") }
  }

  def findNextDirection(col:Int,row:Int,d:Int,v:Int):Int = {
    var i = d + 3
    while(i < d + 7) {
      val m = i % 4
      if(m == 0) {
        // Check left
        if(col > 0) {
          if(r.get(col-1,row) == v) {
            return LEFT
          }
        }
      }
      else if(m == 1) {
        // Check down
        if(row < rows) {
          if(r.get(col,row+1) == v) {
            return DOWN
          }
        }
      }
      else if(m == 2) {
        // Check right
        if(col < cols) {
          if(r.get(col+1,row) == v) {
            return RIGHT
          }
        }
      }
      else if(m == 3) {
        // Check up
        if(row > 0) {
          if(r.get(col,row-1) == v) {
            return UP
          }
        }
      }
      i += 1
    }

    return NOTFOUND
  }

  def getPolygon[T](v:Int, data:T) = {
    val points = mutable.ArrayBuffer[geom.Coordinate]()

    // Find upper left start point.
    var sc = 0
    var sr = 0
    var found = false
    while(!found && sc < cols) {
      sr = 0
      while(!found && sr < rows) {
        if(r.get(sc,sr) == v) { found = true }
        if(!found) { sr += 1 }
      }
      if(!found) { sc += 1 }
    }

    if(!found) { sys.error(s"This raster does not contain value $v") }

    val startCol = sc
    val startRow = sr
    points += mark(startCol,startRow,TOPLEFT)

    // First check down and right of first. 
    var direction = NOTFOUND
    if(startRow < rows) {
      if(r.get(startCol,startRow+1) == v) {
        direction = DOWN
      }
    }

    if(direction == NOTFOUND && startCol < cols) {
      if(r.get(startCol+1,startRow) == v) {
        direction = RIGHT
      }
    }

    if(direction == NOTFOUND) {
      // Single cell polygon.
      points += mark(startCol,startRow,BOTTOMLEFT)
      points += mark(startCol,startRow,BOTTOMRIGHT)
      points += mark(startCol,startRow,TOPRIGHT)
      points += mark(startCol,startRow,TOPLEFT)
    } else {
      var previousDirection = direction
      var col = startCol
      var row = startRow

      var break = false
      while(!break) {
        // Move in the direction.
        if(direction == DOWN) {
          row += 1
        } else if(direction == RIGHT) {
          col += 1
        } else if(direction == UP) {
          row -= 1
        } else if(direction == LEFT) {
          col -= 1
        }

        makeMarks(points, col,row,direction,previousDirection)
        previousDirection = direction
        direction = findNextDirection(col,row,direction,v)
        if(col == startCol && row == startRow) {
          if(previousDirection == LEFT) {
            break = true
          } else if(previousDirection == UP && direction == DOWN) {
            break = true
          }
        }
      }

      // Make end marks
      if(direction == UP) { mark(col,row, TOPRIGHT) }
      points += mark(startCol,startRow,TOPLEFT) // Completes the polygon
    }

    Polygon(points.toArray, data)
  }
}
