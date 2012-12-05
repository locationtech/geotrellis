package geotrellis.feature.rasterize

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.{ geom => jts }
import collection.immutable.TreeMap

object PolygonRasterizer {
  /**
   * Apply a function to each raster cell that intersects with a polygon.
   */
  def foreachCellByPolygon[D](p:Polygon[D], re:RasterExtent, includeExterior:Boolean=false)( f:(Int,Int,Polygon[D]) => Unit) {
    // Create a global edge table which tracks the minimum and maximum row 
    // for which each edge is relevant.
    val edgeTable = buildEdgeTable(p, re)
    val activeEdgeTable = ActiveEdgeTable.empty

    // Process each row in the raster that intersects with the polygon.
    for(row <- edgeTable.rowMin to edgeTable.rowMax) {

     // Update our active edge table to reflect the current row.
     activeEdgeTable.update(row, edgeTable, re)
     
      // activeEdgeTable.updateIntercepts(row, re)

      // call function on included cells
      val fillRanges = activeEdgeTable.fillRanges(row,includeExterior)
      val cellRanges = processRanges(fillRanges)
      for ( (col0, col1) <- cellRanges;
            col          <- col0 to col1
      ) f(col,row,p)

      activeEdgeTable.dropEdges(row)
    }
  }

  /** Return columns to be included in this range.
   * 
   * If includeTouched is true, all cells touched by the polygon will be included.
   * If includeTouched is false, only cells whose center point is within the polygon will be included.
   */
  def processRanges(fillRanges:List[(Double,Double)], includeTouched:Boolean = false):List[(Int,Int)] = {
    for ( (x0, x1) <- fillRanges) yield {
      val cellWidth = 1
      if (includeTouched) {
        ( (math.floor(x0)).toInt, (math.ceil(x1)).toInt )
      } else {
        ( (math.floor(x0 + 0.5)).toInt,  (math.floor(x1 - 0.5)).toInt )
      }
    }
  }

  case class ActiveEdgeTable(var edges:List[Intercept]) {
    def dropEdges(row:Int) {
      edges = edges.filter(_.line.rowMax != row)
    }

    def update(row:Int, edgeTable:EdgeTable, re:RasterExtent) {

      // ** add entering edges.
      //    -- move from ET to AET those edges whose rowMin = row
      val (_, y) = re.gridToMap(0, row)
      this.updateIntercepts(row, re)
      val newEdges = edgeTable.edges
        .getOrElse(row, List[Line]())
        .map( line => Intercept(line, y, re) )
      val allEdges:List[Intercept] = edges ++ newEdges

      // ** Remove from AET those entries for which row = rowMax 
      //     (Important to handle intercepts between two lines that are monotonically increasing/decreasing, as well
      //      y maxima, as well simply to drop edges that are no longer relevant)
      val sortedEdges = allEdges.sortWith( _.x < _.x ) // <-- looks crazy
      edges =
        sortedEdges
    }

    /**
     * Update the x intercepts of each line for the next line.
     */
    def updateIntercepts(row:Int, re:RasterExtent) {
      val (_, y) = re.gridToMap(0, row)
      edges = edges.map { i => Intercept(i.line, y, re) }
    } 

    def fillRanges(row:Int, includeExterior:Boolean):List[(Double,Double)] = {
      val doubleRange = edges.grouped(2).map {r => (r(0).colDouble, r(1).colDouble)}.toList
      doubleRange
    }
      
        
  
    
  }
  object ActiveEdgeTable {
    def empty() = new ActiveEdgeTable(List[Intercept]())

    }

  // Inverse slope: 1/m
  case class Line(rowMin: Int, rowMax: Int, x0:Double, y0:Double, x1:Double, y1:Double, inverseSlope: Double) {
    def horizontal:Boolean = rowMin == rowMax
       
    def intercept(y:Double) = 
      x0 + (y - y0) * inverseSlope
  }

  
  object Line {
    def create(c0:jts.Coordinate, c1:jts.Coordinate, re:RasterExtent):Option[Line] = {
      // Calculate minimum and maximum row of target raster in which a line through the 
      // center intersects with the edge.

      /// Our scan lines begin from the bottom, so let's find the minimum y, and then
      /// determine the first row that intersects with it.
      // Make sure that y0 <= y1 
      val (x0, y0, x1, y1) = if (c0.y < c1.y) {
        (c0.x, c0.y, c1.x, c1.y)
      } else {
        (c1.x, c1.y, c0.x, c0.y)
      }
 
      val minRowDouble = re.mapYToGridDouble(y1)
      val maxRowDouble = re.mapYToGridDouble(y0)

      // If the decimal portion of minRowDouble is <= 0.5, then y0 is in 
      // minimum row whose center line intersects with the line; otherwise, it's
      // floor(minRowDouble) + 1. 
      
      val minRow = (math.floor(re.mapYToGridDouble(y1) + 0.5)).toInt

      // If the decimal portion of maxRowDouble is => 0.5, then y1 is in the 
      // maximum row whose center line intersects with this edge. Otherwise,
      // it's floor(maxRowDouble) - 1 
      val maxRow = (math.floor(re.mapYToGridDouble(y0) - 0.5)).toInt
      val inverseSlope = (x1 - x0).toDouble / (y1 - y0).toDouble

      if (minRow > maxRow || c0.y == c1.y || inverseSlope == java.lang.Double.POSITIVE_INFINITY || inverseSlope == java.lang.Double.NEGATIVE_INFINITY ) // drop horizontal lines 
        None
      else {
        Some(Line(minRow, maxRow, x0, y0, x1, y1, inverseSlope))
      } 
    }
   
    def xIntercept(y:Double, x0:Double, y0:Double, inverseSlope:Double) = 
      x0 + (y - y0) * inverseSlope
  }

  case class Intercept(x:Double, colDouble:Double, line:Line) {
    //def incrementRow:Intercept = { 
    //  Intercept(x + line.inverseSlope, line)
    //}
  }

  object Intercept {
    def apply(line:Line, y:Double, re:RasterExtent) = {
      val x = line.intercept(y) 

      val colDouble = re.mapXToGridDouble(x) 
      new Intercept(x, colDouble, line)
    }
  }

  case class EdgeTable(edges:Map[Int, List[Line]], rowMin:Int, rowMax:Int)

  def buildEdgeTable(p:Polygon[_], re:RasterExtent) = {
    val geom = p.geom 
    val lines = geom.getExteriorRing.getCoordinates.sliding(2).flatMap {  
      case Array(c1,c2) => Line.create(c1,c2,re)
    }.toList

    val rowMin = lines.map( _.rowMin ).reduceLeft( math.min(_, _) ) 
    val rowMax = lines.map( _.rowMax ).reduceLeft( math.max(_, _) ) 

    var map = Map[Int,List[Line]]()
    for(line <- lines) {
      // build lists of lines by starting row
      val linelist = map.get(line.rowMin)
                        .getOrElse(List[Line]())
      map += (line.rowMin -> (linelist :+ line))
    }
    EdgeTable(map,rowMin,rowMax)
  }
  
}
