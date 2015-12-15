/*
 * Copyright (c) 2015 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.rasterize.polygon

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.rasterize._

object PolygonRasterizer {
  import scala.collection.mutable

  val limit : Int = 1000000
  val epsilon : Double = 0.0000001

  /**
   * Given a polygon and a raster extent, return the list of
   * non-horizontal segments -- in raster coordinates -- that
   * comparise the boundary of the polygon.
   *
   * @param poly A polygon
   * @param poly A raster extent
   */
  def polygonToEdges(poly : Polygon, re : RasterExtent) = {
    (poly.exterior :: poly.holes.toList).flatMap({ line =>
      line.points.sliding(2).map({ pointPair =>
        /* Points in map coordinates */
        val (x0, y0) = (pointPair(0).x, pointPair(0).y)
        val (x1, y1) = (pointPair(1).x, pointPair(1).y)
        /* Points in grid coordinates */
        val point1 = Point(re.mapXToGridDouble(x0), re.mapYToGridDouble(y0))
        val point2 = Point(re.mapXToGridDouble(x1), re.mapYToGridDouble(y1))

        if (point1.y < point2.y) Line(point1, point2); else Line(point2, point1)
      })
    })
  }

  /**
   * Given a list of edges, a y-value (the scanline), and a maximum
   * x-coordinate, this function generates a list of left- and
   * right-endpoints for runs of pixels.  When this function is run
   * over all of the rows, the collective output is a rasterized
   * polygon.  This implements part of the traditional scanline
   * algorithm.
   *
   * @param edges A list of active edges
   * @param y     The y-value of the vertical scanline
   * @param maxX  The maximum-possible x-coordinate
   */
  def runsInterior(edges : List[Line], y : Int, maxX : Int) = {
    val row = Line(Point(-limit, y + 0.5), Point(maxX + limit, y + 0.5))
    lazy val perturbedRow1 = Line(Point(-limit, y + 0.5 + epsilon), Point(maxX + limit, y + 0.5 + epsilon))
    lazy val perturbedRow2 = Line(Point(-limit, y + 0.5 - epsilon), Point(maxX + limit, y + 0.5 - epsilon))

    edges
      .filter({ edge => edge.points(0).y != edge.points(1).y })
      .filter({ edge => edge.intersects(row) })
      .map({ edge =>
        val xcoord = (edge & row).as[Point].get.x + 0.5
        (xcoord, edge) })
      /* Handle duplicate x-values by perturbing the scanline.  The variable
       * pair is bound to a tuple of a double (the x-coordinate) and a
       * list of double (x-coordinate), line (edge) tuples. */
      .groupBy(_._1).flatMap({ pair : (Double, List[(Double, Line)]) =>
        if (pair._2.length == 1) List(pair._1)
        else {
          val intersections1 = pair._2.filter(_._2.intersects(perturbedRow1)).length
          val intersections2 = pair._2.filter(_._2.intersects(perturbedRow2)).length
          List.fill(math.max(intersections1, intersections2).toInt)(pair._1)
        } })
      .toList.sortWith(_ < _)
      .grouped(2)
  }

  /**
   * This does much the same things as runsInterior, except that
   * instead of using a scanline, a "scan rectangle" is used.  When
   * this is run over all of the rows, the collective output is
   * collection of pixels which completely cover the input polygon.
   *
   * @param edges A list of active edges
   * @param y     The y-value of the vertical scanline
   * @param maxX  The maximum-possible x-coordinate
   */
  def runsExterior(edges : List[Line], y : Int, maxX : Int) = {
    val (top, bot) = (y + 1, y + 0)
    val row = Polygon(
      Point(-limit, bot),
      Point(-limit, top),
      Point(maxX + limit, top),
      Point(maxX + limit, bot),
      Point(-limit, bot))

    val interactions = edges.filter({ edge => edge.intersects(row) })
      .map({ edge => (edge & row) })
      .filter({ _.as[Line] != None })
      .map({ _.as[Line].get })
      .sortWith({ (line1, line2) => {
        val x1 = math.min(line1.points.head.x, line1.points.last.x)
        val x2 = math.min(line2.points.head.x, line2.points.last.x)
        x1 < x2
      }})

    val low = interactions.filter({ event => {
      val points = event.points
      ((points.head.y == bot) || (points.last.y == bot)) }})
      .grouped(2)

    val middle = interactions.filter({ event => {
      val point = event.points
      ((point.head.y != bot)
        && (point.head.y != top)
        && (point.last.y != bot)
        && (point.last.y != top)) }})

    val high = interactions.filter({ event => {
      val points = event.points
      ((points.head.y == top) || (points.last.y == top)) }})
      .grouped(2)

    val intervals = (
      (low ++ high)
        .map({ case List(line1, line2) => {
          val xs = line1.points.map(_.x) ++ line2.points.map(_.x)
          val min = xs.reduce(math.min(_,_))
          val max = xs.reduce(math.max(_,_))
          List(math.floor(min), math.ceil(max))
        }}) ++
        middle.map({ line => {
          val x0 = line.points.head.x
          val x1 = line.points.last.x
          List(math.floor(math.min(x0,x1)), math.ceil(math.max(x0,x1)))
        }})
    ).toList.sortWith(_.head < _.head)

    /* Merge and return intervals */
    if (intervals.length > 0) {
      val stack = mutable.Stack(intervals.head)
      intervals.tail.foreach({ interval => {
        val (l1,r1) = (stack.top.head, stack.top.last)
        val (l2,r2) = (interval.head, interval.last)
        if (r1 < l2) stack.push(interval)
        else {
          stack.pop
          stack.push(List(l1, math.max(r1,r2)))
        }
      }})
      stack.toList
    } else List.empty
  }

  def foreachCellByPolygon(poly: Polygon, re: RasterExtent, includeExterior: Boolean = false)(f: Callback): Unit = {
    val edges = polygonToEdges(poly, re)

    var y = 0
    while(y < re.rows) {
      val activeEdges =
        (if (includeExterior) runsExterior(edges, y, re.cols)
        else runsInterior(edges, y, re.cols))
          .foreach({ case List(startDouble, stopDouble) => {
            var x = math.max(startDouble.toInt,0)
            val stop = math.min(stopDouble.toInt,re.cols)
            while(x < stop) {
              f(x, y)
              x += 1
            }}})
      y += 1
    }
  }

}
