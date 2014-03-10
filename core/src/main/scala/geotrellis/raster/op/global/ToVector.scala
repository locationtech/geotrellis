package geotrellis.raster.op.global

import geotrellis._
import geotrellis.feature._
import geotrellis.feature.rasterize.{PolygonRasterizer, Callback}

import com.vividsolutions.jts.geom

import scala.collection.mutable

case class ToVector(r:Op[Raster], 
                    regionConnectivity:Connectivity = RegionGroupOptions.default.connectivity)
    extends Operation[List[Polygon[Int]]] {
  def _run() = runAsync('init :: r :: Nil)

  class ToVectorCallback(val polyizer:Polygonizer,
                         val r:Raster,
                         val v:Int) extends Callback[Polygon,Int] {
    val innerStarts = mutable.Map[Int,(Int,Int)]()

    def linearRings = 
      for(k <- innerStarts.keys) yield {
        polyizer.getLinearRing(k, innerStarts(k))
      }

    def apply(col:Int,row:Int,poly:Polygon[Int]) = {
      val innerV = r.get(col,row)
      if(innerV != v) {
        if(innerStarts.contains(innerV)) {
          val (pcol,prow) = innerStarts(innerV)
          if(col < pcol || ((col == pcol) && row < prow)) {
            innerStarts(innerV) = (col,row)
          }
        } else {
          innerStarts(innerV) = (col,row)
        }
      }
    }
  }

  val nextSteps:Steps = {
    case 'init :: a :: Nil => 
      val regionGroupOptions = 
        RegionGroupOptions(
          connectivity = regionConnectivity,
          ignoreNoData = false
        )
      runAsync('regioned :: RegionGroup(r,regionGroupOptions) :: Nil)
    case 'regioned :: (rgr:RegionGroupResult) :: Nil =>
      val polyizer = new Polygonizer(rgr.raster)
      val r = rgr.raster
      val regionMap = rgr.regionMap
      val processedValues = mutable.Set[Int]()
      val polygons = mutable.Set[Polygon[Int]]()

      var col = 0
      while(col < r.cols) {
        var row = 0
        while(row < r.rows) {
          val v = r.get(col,row)
          if(isData(regionMap(v))) {
            if(!processedValues.contains(v)) {
              val shell = polyizer.getLinearRing(v,(col,row))
              val shellPoly = Polygon(
                new geom.Polygon(shell, Array[geom.LinearRing](), Feature.factory),
                rgr.regionMap(v)
              )

              val callback = new ToVectorCallback(polyizer,r,v)

              PolygonRasterizer.foreachCellByPolygon(shellPoly, r.rasterExtent)(callback)

              polygons += Polygon(
                new geom.Polygon(shell, callback.linearRings.toArray, Feature.factory),
                rgr.regionMap(v)
              )

              processedValues += v
            }
          }
          row += 1
        }
        col += 1
      }

      Result(polygons.toList)
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
          points += mark(col,row-1,TOPRIGHT)
          points += mark(col,row-1,TOPLEFT)
        }
      }
    } else if(d == RIGHT) {
      if(pd != RIGHT) {
        if(pd == UP) {                   //RHT
          points += mark(col-1,row,BOTTOMRIGHT)
        } else if(pd == DOWN) {          //LHT
          points += mark(col-1,row,BOTTOMLEFT)
        } else {                         //RT
          points += mark(col-1,row,TOPLEFT)
          points += mark(col-1,row,BOTTOMLEFT)
        }
      }
    } else if(d == UP) {
      if(pd != UP) {
        if(pd == LEFT) {                 //RHT
          points += mark(col,row+1,TOPRIGHT)
        } else if(pd == RIGHT) {         //LHT
          points += mark(col,row+1,BOTTOMRIGHT)
        } else {                         //RT
          points += mark(col,row+1,BOTTOMLEFT)
          points += mark(col,row+1,BOTTOMRIGHT)
        }
      }
    } else if(d == LEFT) {
      if(pd != LEFT) {
        if(pd == DOWN) {                 //RHT
          points += mark(col+1,row,TOPLEFT)
        } else if(pd == UP) {            //LHT
          points += mark(col+1,row,TOPRIGHT)
        } else {                         //RT
          points += mark(col+1,row,BOTTOMRIGHT)
          points += mark(col+1,row,TOPRIGHT)
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
        if(row+1 < rows) {
          if(r.get(col,row+1) == v) {
            return DOWN
          }
        }
      }
      else if(m == 2) {
        // Check right
        if(col+1 < cols) {
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

  def getPolygon[T](v:Int, data:T):Polygon[T] = {
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
    getPolygon(v,data,(sc,sr))
  }

  def getPolygon[T](v:Int, data:T,startPoint:(Int,Int)):Polygon[T] = {
    val shell = getLinearRing(v:Int,startPoint:(Int,Int))
    Polygon(Feature.factory.createPolygon(shell, Array()),data)
  }

  def getLinearRing[T](v:Int, startPoint:(Int,Int)) = {
    val points = mutable.ArrayBuffer[geom.Coordinate]()

    val startCol = startPoint._1
    val startRow = startPoint._2

    // First check down and right of first. 
    var direction = NOTFOUND
    if(startRow+1 < rows) {
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
      points += mark(startCol,startRow,TOPLEFT)
      points += mark(startCol,startRow,BOTTOMLEFT)
      points += mark(startCol,startRow,BOTTOMRIGHT)
      points += mark(startCol,startRow,TOPRIGHT)
      points += mark(startCol,startRow,TOPLEFT)
    } else {
      points += mark(startCol,startRow,TOPLEFT)
      if(direction == RIGHT) { points += mark(startCol,startRow,BOTTOMLEFT) }

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

        makeMarks(points, col, row, direction, previousDirection)
        previousDirection = direction
        direction = findNextDirection(col, row, direction, v)
        //println(s"PREVIOUS ${sd(previousDirection)} NEXT ${sd(direction)}  ($col,$row)")
        if(col == startCol && row == startRow) {
          if(previousDirection == LEFT || previousDirection == DOWN) {
            break = true
          } else if((previousDirection == UP || previousDirection == RIGHT) && 
                    (direction == DOWN || direction == LEFT)) {
            break = true
          }
        }
      }

      // Make end marks
      if(previousDirection == UP) { points += mark(col,row, TOPRIGHT) }
      points += mark(startCol,startRow,TOPLEFT) // Completes the ring
    }

    Feature.factory.createLinearRing(points.toArray)
  }
}
