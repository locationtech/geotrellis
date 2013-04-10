package geotrellis.feature.rasterize

import language.higherKinds

import geotrellis.feature._
import geotrellis._

trait Callback[G[_],T] {
  def apply(col: Int, row: Int, g: G[T])
}

trait Transformer[G[_],A,B] {
  def apply(col: Int, row: Int, g: G[A]):B
}


object Rasterizer {

  /**
   * Create a raster from a geometry feature.
   * @param feature       Feature to rasterize
   * @param rasterExtent  Definition of raster to create
   * @param f             Function that returns single value to burn
   */ 
  def rasterizeWithValue[D](feature:Geometry[D], rasterExtent:RasterExtent)(f:(D) => Int) = {
    val cols = rasterExtent.cols
    val array = Array.fill[Int](rasterExtent.cols * rasterExtent.rows)(NODATA)
    val burnValue = f(feature.data)
    val f2 = new Callback[Geometry,D] {
        def apply(col: Int, row: Int, g: Geometry[D]) {
          array(row * cols + col) = burnValue
        }
      }
    foreachCellByFeature(feature, rasterExtent)(f2) 
    Raster(array,rasterExtent)
  } 
  /**
   * Create a raster from a geometry feature.
   * @param feature       Feature to rasterize
   * @param rasterExtent  Definition of raster to create
   * @param f             Function that takes col, row, feature and returns value to burn
   */ 
  def rasterize[D](feature:Geometry[D], rasterExtent:RasterExtent)(f:Transformer[Geometry,D,Int]) = {
    val cols = rasterExtent.cols
    val array = Array.fill[Int](rasterExtent.cols * rasterExtent.rows)(NODATA)
    val f2 = new Callback[Geometry,D] {
        def apply(col: Int, row: Int, polygon: Geometry[D]) {
          array(row * cols + col) = f(col,row,polygon)
        }
    }
    foreachCellByFeature(feature, rasterExtent)(f2)
    Raster(array,rasterExtent)
  }
   
  /**
   * Perform a zonal summary by invoking a function on each cell under provided features.
   *
   * This function is a closure that returns Unit; all results are a side effect of this function.
   * 
   * Note: the function f should modify a mutable variable as a side effect.  
   * While not ideal, this avoids the unavoidable boxing that occurs when a 
   * Function3 returns a primitive value.
   * 
   * @param feature  Feature for calculation
   * @param re       RasterExtent to use for iterating through cells
   * @param f        A function that takes (col:Int, row:Int, rasterValue:Int, feature:Feature)
   */
  def foreachCellByFeature[G[_] <: Geometry[_], D](feature:G[D], re:RasterExtent)(f: Callback[G,D]):Unit = {
    feature match {
      case p:Point[_] => foreachCellByPoint[D](p.asInstanceOf[Point[D]],re)(f.asInstanceOf[Callback[Point,D]])
      case p:MultiPoint[_] => foreachCellByMultiPoint[D](p.asInstanceOf[MultiPoint[D]],re)(f.asInstanceOf[Callback[Point,D]])
      case p:MultiLineString[_] => foreachCellByMultiLineString[D](p.asInstanceOf[MultiLineString[D]],re)(f.asInstanceOf[Callback[LineString,D]])
      case p:LineString[_] => foreachCellByLineString[D](p.asInstanceOf[LineString[D]],re)(f.asInstanceOf[Callback[LineString,D]])
      case p:Polygon[_] => PolygonRasterizer.foreachCellByPolygon[D](p.asInstanceOf[Polygon[D]],re)(f.asInstanceOf[Callback[Polygon,D]])
      case p:MultiPolygon[_] => foreachCellByMultiPolygon[D](p.asInstanceOf[MultiPolygon[D]],re)(f.asInstanceOf[Callback[Polygon,D]])
      case _ => ()
    }
  }
    
  /**
   * Invoke a function on raster cells under a point feature.
   * 
   * The function f is a closure that should alter a mutable variable by side
   * effect (to avoid boxing).  
   */
  def foreachCellByPoint[D](p:Point[D], re:RasterExtent)(f:Callback[Point,D]) {
    val geom = p.geom
    val col = re.mapXToGrid(geom.getX())
    val row = re.mapYToGrid(geom.getY())
    f(col,row,p)
  }

  def foreachCellByMultiPoint[D](p:MultiPoint[D], re:RasterExtent)(f: Callback[Point,D]) {
    p.flatten.foreach(foreachCellByPoint(_, re)(f))
  }

  /**
   * Invoke a function on each point in a sequences of Points.
   */
  def foreachCellByPointSeq[D](pSet:Seq[Point[D]], re:RasterExtent)(f: Callback[Point,D]) {
    pSet.foreach(foreachCellByPoint(_,re)(f))
  }
  
  /**
   * Apply function f to every cell contained within MultiLineString.
   * @param g   MultiLineString used to define zone
   * @param re  RasterExtent used to determine cols and rows
   * @param f   Function to apply: f(cols,row,feature)
   */
  def foreachCellByMultiLineString[D](g:MultiLineString[D], re:RasterExtent)(f: Callback[LineString,D]) {
    g.flatten.foreach(foreachCellByLineString(_,re)(f))
  }

  /**
   * Apply function f(col,row,feature) to every cell contained within polygon.
   * @param p   Polygon used to define zone
   * @param re  RasterExtent used to determine cols and rows
   * @param f   Function to apply: f(cols,row,feature)
   */
  def foreachCellByPolygon[D](p:Polygon[D], re:RasterExtent)(f: Callback[Polygon, D]) {
     PolygonRasterizer.foreachCellByPolygon(p, re)(f)
  }

  /**
   * Apply function f to every cell contained with MultiPolygon.
   *
   * @param p   MultiPolygon used to define zone
   * @param re  RasterExtent used to determine cols and rows
   * @param f   Function to apply: f(cols,row,feature)
   */
  def foreachCellByMultiPolygon[D](p:MultiPolygon[D], re:RasterExtent)(f: Callback[Polygon,D]) {
    p.flatten.foreach(PolygonRasterizer.foreachCellByPolygon(_,re)(f))
  }

  /**
   * lines
   */
  //TODO: implement midpoint line algorithm
  // Converts start and end point to grid cells, and draws a line between those two grid cells.
  // Uses DDA or simple incremental algorithm.
  def foreachCellByLineString[D](p:LineString[D], re:RasterExtent)(f: Callback[LineString,D]) {
    val geom = p.geom
    val p0 = geom.getCoordinateN(0)
    val p1 = geom.getCoordinateN(1)

    val p0x = re.mapXToGrid(p0.x)
    val p0y = re.mapYToGrid(p0.y)
    val p1x = re.mapXToGrid(p1.x)
    val p1y = re.mapYToGrid(p1.y)
    
    if (p0x < p1x) {
      foreachCellInGridLine(p0x, p0y, p1x, p1y, p, re)(f)
    } else {
      foreachCellInGridLine(p1x, p1y, p0x, p0y, p, re)(f)
    }
  }
  
  //TODO: optimizations, including getting line within raster extent
  //test for horizontal and vertical lines
  private def foreachCellInGridLine[D](x0:Int, y0:Int, x1:Int, y1:Int, p:LineString[D], re:RasterExtent)(f:Callback[LineString,D]) { 
    val dy = y1 - y0
    val dx = x1 - x0
    val m:Double = dy.toDouble / dx.toDouble
    
    // if a step in x creates a step in y that is greater than one, reverse
    // roles of x and y.
    if (math.abs(m) > 1) {      
      foreachCellInGridLineSteep[D](x0, y0, x1, y1, p, re)(f)
    } else {
      var x:Int = x0
      var y:Double = y0
      
      val ymax = re.extent.ymax
      val ymin = re.extent.ymin
      val xmax = re.extent.xmax
      val xmin = re.extent.xmin

      val cols = re.cols
      val rows = re.rows

      while(x <= x1) {
        //println("x: %d, y: %f".format(x,y))

        val newY:Int = (y + 0.5).asInstanceOf[Int]
        if (x >= 0 && y >= 0 && x < cols && y < rows) {
          f(x, newY, p)
        }
        y += m
        x = x + 1
      }
    }
  }
  
  private def foreachCellInGridLineSteep[D](x0:Int, y0:Int, x1:Int, y1:Int, p:LineString[D], re:RasterExtent)(f: Callback[LineString,D]) {
    val dy = y1 - y0
    val dx = x1 - x0
    val m:Double = dy.toDouble / dx.toDouble
    
    // if a step in x creates a step in y that is greater than one, reverse
    // roles of x and y.
    //if (math.abs(m) > 1) {      
    //  foreachCellInGridLineSteep[D](x0, y0, x1, y1, p, re)(f)
   // }
    
    var x:Double = x0
    var y:Int = y0
    
    val ymax = re.extent.ymax
    val ymin = re.extent.ymin
    val xmax = re.extent.xmax
    val xmin = re.extent.xmin
    
    val cols = re.cols
    val rows = re.rows
    
    val step:Double = 1.0/m
    while(y <= y1) {
      val newX:Int = (x + 0.5).asInstanceOf[Int]
      if (x >= 0 && y >= 0 && x < cols && y < rows) {        
    	  f(newX, y, p)
      }
      x += step
      y = y + 1
    }
  } 
}
