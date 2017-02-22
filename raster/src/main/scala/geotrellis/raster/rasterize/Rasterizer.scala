/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.rasterize

import geotrellis.raster._
import geotrellis.raster.rasterize.extent.ExtentRasterizer
import geotrellis.raster.rasterize.polygon.PolygonRasterizer
import geotrellis.vector._

import spire.syntax.cfor._

import scala.language.higherKinds


trait Transformer[+B] {
  def apply(col: Int, row: Int): B
}

/**
  * An object holding rasterizer functions.
  */
object Rasterizer {

  /**
    * A type encoding rasterizer options.
    */
  case class Options(
    includePartial: Boolean,
    sampleType: PixelSampleType
  )

  /**
    * A companion object for the [[Options]] type.  Includes a
    * function to produce the default options settings.
    */
  object Options {
    def DEFAULT = Options(includePartial = true, sampleType = PixelIsPoint)
  }

  /**
    * Create a raster from a geometry feature.
    *
    * @param geom          Geometry to rasterize
    * @param rasterExtent  Definition of raster to create
    * @param value         Single value to burn
    */
  def rasterizeWithValue(geom: Geometry, rasterExtent: RasterExtent, value: Int): Tile = {
    val cols = rasterExtent.cols
    val array = Array.ofDim[Int](rasterExtent.cols * rasterExtent.rows).fill(NODATA)
    val f2 = (col: Int, row: Int) =>
          array(row * cols + col) = value
    foreachCellByGeometry(geom, rasterExtent)(f2)
    ArrayTile(array, rasterExtent.cols, rasterExtent.rows)
  }

  /**
    * Create a raster from a geometry feature.
    *
    * @param feature       Feature to rasterize
    * @param rasterExtent  Definition of raster to create
    * @param f             Function that takes col, row, feature and returns value to burn
    */
  def rasterize(feature: Geometry, rasterExtent: RasterExtent)(f: (Int, Int) => Int) = {
    val cols = rasterExtent.cols
    val array = Array.ofDim[Int](rasterExtent.cols * rasterExtent.rows).fill(NODATA)
    val f2 = (col: Int, row: Int) =>
          array(row * cols + col) = f(col, row)
    foreachCellByGeometry(feature, rasterExtent)(f2)
    ArrayTile(array, rasterExtent.cols, rasterExtent.rows)
  }

  /**
    * Given a Geometry and a [[RasterExtent]], call the function 'f'
    * at each pixel in the raster extent covered by the geometry.  The
    * two arguments to the function 'f' are the column and row.
    */
  def foreachCellByGeometry(geom: Geometry, re: RasterExtent)(f: (Int, Int) => Unit): Unit =
    foreachCellByGeometry(geom, re, Options.DEFAULT)(f)

  /**
    * Perform a zonal summary by invoking a function on each cell
    * under provided features.
    *
    * This function is a closure that returns Unit; all results are a
    * side effect of this function.
    *
    * Note: the function f should modify a mutable variable as a side
    * effect.  While not ideal, this avoids the unavoidable boxing
    * that occurs when a Function3 returns a primitive value.
    *
    * @param geom     Feature for calculation
    * @param re       RasterExtent to use for iterating through cells
    * @param options  Options for the (Multi)Polygon and Extent rasterizers
    * @param f        A function that takes (col: Int, row: Int) and produces nothing
    */
  def foreachCellByGeometry(geom: Geometry, re: RasterExtent, options : Options)(f: (Int, Int) => Unit): Unit = {
    geom match {
      case geom: Point         => foreachCellByPoint(geom, re)(f)
      case geom: MultiPoint    => foreachCellByMultiPoint(geom, re)(f)
      case geom: MultiLine     => foreachCellByMultiLineString(geom, re)(f)
      case geom: Line          => foreachCellByLineString(geom, re)(f)
      case geom: Polygon       => PolygonRasterizer.foreachCellByPolygon(geom, re, options)(f)
      case geom: MultiPolygon  => foreachCellByMultiPolygon(geom, re, options)(f)
      case geom: GeometryCollection => geom.geometries.foreach(foreachCellByGeometry(_, re, options)(f))
      case geom: Extent        => ExtentRasterizer.foreachCellByExtent(geom, re, options)(f)
    }
  }

  /**
    * Invoke a function on raster cells under a point feature.
    *
    * The function f is a closure that should alter a mutable variable
    * by side effect (to avoid boxing).
    */
  def foreachCellByPoint(geom: Point, re: RasterExtent)(f: (Int, Int) => Unit) {
    val col = re.mapXToGrid(geom.x)
    val row = re.mapYToGrid(geom.y)
    f(col, row)
  }

  /**
    * Given a MultiPoint and a [[RasterExtent]], call the function 'f'
    * at each pixel in the raster extent covered by the geometry.  The
    * two arguments to the function 'f' are the column and row.
    */
  def foreachCellByMultiPoint(p: MultiPoint, re: RasterExtent)(f: (Int, Int) => Unit) {
    p.points.foreach(foreachCellByPoint(_, re)(f))
  }

  /**
    * Invoke a function on each point in a sequences of Points.
    */
  def foreachCellByPointSeq(pSet: Seq[Point], re: RasterExtent)(f: (Int, Int) => Unit) {
    pSet.foreach(foreachCellByPoint(_, re)(f))
  }

  /**
    * Apply function f to every cell contained within MultiLineString.
    *
    * @param g   MultiLineString used to define zone
    * @param re  RasterExtent used to determine cols and rows
    * @param f   Function to apply: f(cols, row, feature)
    */
  def foreachCellByMultiLineString(g: MultiLine, re: RasterExtent)(f: (Int, Int) => Unit) {
    g.lines.foreach(foreachCellByLineString(_, re)(f))
  }

  /**
    * Given a Polygon and a [[RasterExtent]], call the function 'f' at
    * each pixel in the raster extent covered by the geometry.  The
    * two arguments to the function 'f' are the column and row.
    */
  def foreachCellByPolygon(p: Polygon, re: RasterExtent)(f: (Int, Int) => Unit): Unit =
    foreachCellByPolygon(p, re, Options.DEFAULT)(f)

  /**
    * Apply function f(col, row, feature) to every cell contained
    * within polygon.
    *
    * @param p        Polygon used to define zone
    * @param re       RasterExtent used to determine cols and rows
    * @param options  The options parameter controls whether to treat pixels as points or areas and whether to report partially-intersected areas.
    * @param f        Function to apply: f(cols, row, feature)
    */
  def foreachCellByPolygon(p: Polygon, re: RasterExtent, options: Options)(f: (Int, Int) => Unit) {
     PolygonRasterizer.foreachCellByPolygon(p, re, options)(f)
  }

  /**
    * Given a MultiPolygon and a [[RasterExtent]], call the function
    * 'f' at each pixel in the raster extent covered by the geometry.
    * The two arguments to the function 'f' are the column and row.
    */
  def foreachCellByMultiPolygon[D](p: MultiPolygon, re: RasterExtent)(f: (Int, Int) => Unit): Unit =
    foreachCellByMultiPolygon(p, re, Options.DEFAULT)(f)

  /**
   * Apply function f to every cell contained with MultiPolygon.
   *
   * @param p        MultiPolygon used to define zone
   * @param re       RasterExtent used to determine cols and rows
   * @param options  The options parameter controls whether to treat pixels as points or areas and whether to report partially-intersected areas.
   * @param f        Function to apply: f(cols, row, feature)
   */
  def foreachCellByMultiPolygon[D](p: MultiPolygon, re: RasterExtent, options: Options)(f: (Int, Int) => Unit) {
    p.polygons.foreach(PolygonRasterizer.foreachCellByPolygon(_, re, options)(f))
  }

  /**
    * Iterates over the cells determined by the segments of a
    * LineString.  The iteration happens in the direction from the
    * first point to the last point.
    */
  def foreachCellByLineString(line: Line, re: RasterExtent)(f: (Int, Int) => Unit) {
    val cells = (for(coord <- line.jtsGeom.getCoordinates()) yield {
      (re.mapXToGrid(coord.x), re.mapYToGrid(coord.y))
    }).toList

    for(i <- 1 until cells.size) {
      foreachCellInGridLine(cells(i - 1)._1,
                            cells(i - 1)._2,
                            cells(i)._1,
                            cells(i)._2, line, re, i != cells.size - 1)(f)
    }
  }

  /**
    * Implementation of the Bresenham line drawing algorithm.  Only
    * calls on cell coordinates within raster extent.
    *
    * The parameter 'skipLast' is a flag which is 'true' if the
    * function should skip function calling the last cell (x1, y1) and
    * false otherwise.  This is useful for not duplicating end points
    * when calling for multiple line segments.
    *
    * @param    p                  LineString used to define zone
    * @param    re                 RasterExtent used to determine cols and rows
    * @param    skipLast           Boolean flag
    * @param    f                  Function to apply: f(cols, row, feature)
    */
  def foreachCellInGridLine[D](
    x0: Int, y0: Int,
    x1: Int, y1: Int,
    p: Line, re: RasterExtent, skipLast: Boolean = false
  )(f: (Int, Int) => Unit) = {
    val dx=math.abs(x1 - x0)
    val sx=if (x0 < x1) 1 else -1
    val dy=math.abs(y1 - y0)
    val sy=if (y0 < y1) 1 else -1

    var x = x0
    var y = y0
    var err = (if (dx>dy) dx else -dy) / 2
    var e2 = err

    while(x != x1 || y != y1){
      if(0 <= x && x < re.cols &&
         0 <= y && y < re.rows) { f(x, y); }
      e2 = err
      if (e2 > -dx) { err -= dy; x += sx; }
      if (e2 < dy) { err += dx; y += sy; }
    }
    if(!skipLast &&
       0 <= x && x < re.cols &&
       0 <= y && y < re.rows) { f(x, y); }
  }
}
