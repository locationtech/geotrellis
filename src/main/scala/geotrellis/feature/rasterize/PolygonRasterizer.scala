package geotrellis.feature.rasterize

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.{ geom => jts }
import collection.immutable.TreeMap

// 1) Add each line to a global edge table.

//    a) Calculate minimum and maximum row of target raster in which a line through the 
//       center intersects with the edge.

object PolygonRasterizer {
  /**
   *
   */
  def foreachCellByPolygon[D](p:Polygon[D], re:RasterExtent, f:(Int,Int,Polygon[D]) => Unit, includeExterior:Boolean = true) {
    println("starting foreachCellByPolygon")
    val edgeTable = buildEdgeTable(p, re)
    val activeEdgeTable = ActiveEdgeTable.empty

    println("edgeTable: " + edgeTable)
    println("edgeTable, rowMin: " + edgeTable.rowMin)
    println("edgeTable, rowMax: " + edgeTable.rowMax)
    for(row <- edgeTable.rowMin to edgeTable.rowMax) {
     println("processing row: " + row)
     activeEdgeTable.update(row, edgeTable, re)
     activeEdgeTable.updateIntercepts(row, re)

     println("after activeEdgeTable update: " + activeEdgeTable)

      //TODO: exclude col0 & col1
      // call function on included cells
      val fillRanges = activeEdgeTable.fillRanges(row,includeExterior)
      println("fillRanges: " + fillRanges)
      val cellRanges = processRanges(fillRanges)
      for ( (col0, col1) <- cellRanges;
            col          <- col0 to col1
      ) f(col,row,p)

      activeEdgeTable.dropEdges(row)
    }
  }

  def processRanges(fillRanges:List[(Double,Double)]):List[(Int,Int)] = {
    for ( (x0, x1) <- fillRanges) yield {
      println("processing range: " + x0 + "," + x1)
      val cellWidth = 1
      val minCol = (math.floor(x0 + 0.5)).toInt
      val maxCol = (math.floor(x1 - 0.5)).toInt
      //val col0:Int = if (x0 % cellWidth >= .5) {
      //  math.ceil(x0).toInt
      //} else {
      //  math.floor(x0).toInt
     // }
      //val col1:Int = if (x1 % cellWidth <= .5) {
      //  math.floor(x1).toInt
      //} else {
      //  math.ceil(x1).toInt
      //}.toInt
      println("result range: " + minCol + "," + maxCol)
      (minCol,maxCol)
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
      println("running update for row: " + row + ", at y: " + y)

      val newEdges = edgeTable.edges
        .getOrElse(row, List[Line]())
        .map( line => Intercept(line, y, re) )
      //println("new edges will be: " + newEdges)
      val allEdges:List[Intercept] = edges ++ newEdges
      //println("new edges plus old edges is: " + allEdges)

      // ** Remove from AET those entries for which row = rowMax 
      //     (Important to handle intercepts between two lines that are monotonically increasing/decreasing, as well
      //      y maxima, as well simply to drop edges that are no longer relevant)
      //println("about to filter for row: " + row)
     // println("filteredEdges: "  + filteredEdges)
      val sortedEdges = allEdges.sortWith( _.x < _.x ) // <-- looks crazy
      println("sortedEdges: " + sortedEdges)
      edges = //if (includeExterior)
        sortedEdges
      //else 
      //  sortedEdges.filter( edge => row != edge.line.rowMax ) 
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
      //else { 
      // edges.grouped(2).map {r => (r(0).col + 1, r(1).col - 1)}.filter( a => a._1 <= a._2 ).toList
      //}
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

      if (c0.y == c1.y || inverseSlope == java.lang.Double.POSITIVE_INFINITY || inverseSlope == java.lang.Double.NEGATIVE_INFINITY ) // drop horizontal lines 
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
      println("creating intercept for y: " + y +  " --> " + x)

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

// vim: set ts=4 sw=4 et:
