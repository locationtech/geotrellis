package geotrellis.feature.rasterize

import geotrellis._
import geotrellis.feature._
import com.vividsolutions.jts.{ geom => jts }
import collection.immutable.TreeMap

object PolygonRasterizer {
  /**
   *
   */
  def foreachCellByPolygon[D](p:Polygon[D], re:RasterExtent, f:(Int,Int,Polygon[D]) => Unit, includeExterior:Boolean = true) {
    val edgeTable = buildEdgeTable(p, re)
    val activeEdgeTable = ActiveEdgeTable.empty

    //println("edgeTable: " + edgeTable)
    for(row <- edgeTable.rowMin to edgeTable.rowMax) {
     println("processing row: " + row)
     activeEdgeTable.update(row, edgeTable, includeExterior)
     println("after activeEdgeTable update: " + activeEdgeTable)

      //TODO: exclude col0 & col1
      // call function on included cells
      for ( (col0, col1) <- activeEdgeTable.fillRanges(row,includeExterior);
            col          <- col0 to col1
      ) f(col,row,p)
     
      activeEdgeTable.dropEdges(row)
      activeEdgeTable.updateIntercepts
    }
  }


  case class ActiveEdgeTable(var edges:List[Intercept]) {
    def dropEdges(row:Int) {
      edges = edges.filter(_.line.rowMax != row)
    }

    def update(row:Int, edgeTable:EdgeTable, includeExterior:Boolean) {
      // ** add entering edges.
      //    -- move from ET to AET those edges whose rowMin = row
      val newEdges = edgeTable.edges
        .getOrElse(row, List[Line]())
        .map( line => Intercept(line) )
      //println("new edges will be: " + newEdges)
      val allEdges:List[Intercept] = edges ++ newEdges
      //println("new edges plus old edges is: " + allEdges)

      // ** Remove from AET those entries for which row = rowMax 
      //     (Important to handle intercepts between two lines that are monotonically increasing/decreasing, as well
      //      y maxima, as well simply to drop edges that are no longer relevant)
      //println("about to filter for row: " + row)
     // println("filteredEdges: "  + filteredEdges)
      val sortedEdges = allEdges.sortWith( _.col < _.col ) // <-- looks crazy
      //println("sortedEdges: " + sortedEdges)
      edges = if (includeExterior)
        sortedEdges
      else 
        sortedEdges.filter( edge => row != edge.line.rowMax ) 
    }

    /**
     * Update the x intercepts of each line for the next line.
     */
    def updateIntercepts {
      edges = edges.map { _.incrementRow }
    } 

    def fillRanges(row:Int, includeExterior:Boolean):List[(Int,Int)] = {
      val doubleRange = if (includeExterior)  
        edges.grouped(2).map {r => (r(0).col, r(1).col)}.toList
      else { 
        edges.grouped(2).map {r => (r(0).col + 1, r(1).col - 1)}.filter( a => a._1 <= a._2 ).toList
      }
      doubleRange.map( a => (a._1.toInt, a._2.toInt) )
    }
      
        
  
    
  }
  object ActiveEdgeTable {
    def empty() = new ActiveEdgeTable(List[Intercept]())

    }

  // Inverse slope: 1/m
  case class Line(rowMin: Int, rowMax: Int, minCol:Int, inverseSlope: Double) {
    def horizontal:Boolean = rowMin == rowMax
  }

  
  object Line {
    def create(c0:jts.Coordinate, c1:jts.Coordinate, re:RasterExtent):Option[Line] = {
      val x0 = c0.x
      val y0 = c0.y
      val x1 = c1.x
      val y1 = c1.y 
      
      val col0 = re.mapXToGrid(x0)
      val row0 = re.mapYToGrid(y0)
      val col1 = re.mapXToGrid(x1)
      val row1 = re.mapYToGrid(y1)

      //TODO: Evaluate using the slope of the original line versus using the slope
      //      of the line converted into grid coordinates
      if (row0 == row1) // drop horizontal lines 
        None
      else {
        val inverseSlope = (col1 - col0).toDouble / (row1 - row0).toDouble
        Some(Line(math.min(row0, row1), math.max(row0, row1), math.min(col0, col1), inverseSlope))
      } 
    }
  }

  case class Intercept(col:Double, line:Line) {
    def incrementRow:Intercept = Intercept(col + line.inverseSlope, line)
  }

  object Intercept {
    def apply(line:Line) = new Intercept(line.minCol, line)
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
