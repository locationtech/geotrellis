package geotrellis.feature.rasterize

import language.higherKinds

import geotrellis.feature._
import geotrellis._

trait Callback[-G[_],T] {
  def apply(col: Int, row: Int, g: G[T])
}

trait Transformer[-G[_],A,+B] {
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
   * Iterates over the cells determined by the segments of a LineString.
   * The iteration happens in the direction from the first point to the last point.
   */
  def foreachCellByLineString[D](p:LineString[D], re:RasterExtent)(f: Callback[LineString,D]) {
    val geom = p.geom

    val cells = (for(coord <- geom.getCoordinates()) yield { 
      (re.mapXToGrid(coord.x), re.mapYToGrid(coord.y)) 
    }).toList

    for(i <- 1 until cells.length) {
      foreachCellInGridLine(cells(i-1)._1, 
                            cells(i-1)._2, 
                            cells(i)._1, 
                            cells(i)._2, p, re, i != cells.length - 1)(f)
    }
  }

  /***
   * Implementation of the Bresenham line drawing algorithm.
   * Only calls on cell coordinates within raster extent.
   *
   * @param    p                  LineString used to define zone
   * @param    re                 RasterExtent used to determine cols and rows
   * @param    skipLast           'true' if the function should skip function calling the last cell (x1,y1).
   *                              This is useful for not duplicating end points when calling for multiple
   *                              line segments
   * @param    f                  Function to apply: f(cols,row,feature)
   */
  def foreachCellInGridLine[D](x0:Int, y0:Int, x1:Int, y1:Int, p:LineString[D], re:RasterExtent, skipLast:Boolean = false)
                              (f:Callback[LineString,D]) = {
    val dx=math.abs(x1-x0)
    val sx=if (x0<x1) 1 else -1
    val dy=math.abs(y1-y0)
    val sy=if (y0<y1) 1 else -1
    
    var x = x0
    var y = y0
    var err = (if (dx>dy) dx else -dy)/2
    var e2 = err

    while(x != x1 || y != y1){
      if(0 <= x && x < re.cols &&
         0 <= y && y < re.rows) { f(x,y,p); }
      e2 = err;
      if (e2 > -dx) { err -= dy; x += sx; }
      if (e2 < dy) { err += dx; y += sy; }
    }
    if(!skipLast &&
       0 <= x && x < re.cols &&
       0 <= y && y < re.rows) { f(x,y,p); }
  }
}
