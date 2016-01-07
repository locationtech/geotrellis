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

import spire.syntax.cfor._

import scala.collection.JavaConverters._
import com.vividsolutions.jts.index.strtree.STRtree
import com.vividsolutions.jts.geom.Envelope

object PolygonRasterizer {
  import scala.collection.mutable
  import math.{abs,min,max}

  type Segment = (Double, Double, Double, Double)

  /**
   * Given a Segment and a y-value (for a line parallel to the
   * x-axis), return the point of intersection and whether it occurs
   * at the top, middle, or bottom of the line segment.
   *
   * @param line  A line segment
   * @param y     The y-value for a line parallel to the x-axis
   */
  private def lineAxisIntersection(line : Segment, y : Double) = {
    val (x1, y1, x2, y2) = line

    if (y == y1) (x1,1) // Top endpoint
    else if (y == y2) (x2,-1) // Bottom endpoint
    else if ((min(y1,y2) <= y) && (y <= max(y1,y2))) { // Between endpoints
      (((x1-x2)*y-(x1*y2-y1*x2))/(y1-y2),0)
    }
    else (Double.NegativeInfinity, 8) // No intersection
  }

  /**
   * Given a polygon and a raster extent, return an R-Tree (being used
   * as an Interval Tree) containing the Segments -- in raster
   * coordinates -- that comprise the boundary of the polygon.
   *
   * @param poly  A polygon
   * @param re    A raster extent
   */
  def polygonToEdges(poly: Polygon, re: RasterExtent): STRtree = {

    val rtree = new STRtree

    /* Find the outer ring's segments */
    val coords = poly.jtsGeom.getExteriorRing.getCoordinates
    cfor(1)(_ < coords.length, _ + 1) { ci =>
      val coord1 = coords(ci - 1)
      val coord2 = coords(ci)

      val col1 = re.mapXToGridDouble(coord1.x)
      val row1 = re.mapYToGridDouble(coord1.y)
      val col2 = re.mapXToGridDouble(coord2.x)
      val row2 = re.mapYToGridDouble(coord2.y)

      val segment =
        if (row1 < row2) (col1, row1, col2, row2)
        else (col2, row2, col1, row1)

      rtree.insert(new Envelope(min(col1, col2), max(col1, col2), segment._2, segment._4), segment)
    }

    /* Find the segments for the holes */
    cfor(0)(_ < poly.numberOfHoles, _ + 1) { i =>
      val coords = poly.jtsGeom.getInteriorRingN(i).getCoordinates
      cfor(1)(_ < coords.length, _ + 1) { ci =>
        val coord1 = coords(ci - 1)
        val coord2 = coords(ci)

        val col1 = re.mapXToGridDouble(coord1.x)
        val row1 = re.mapYToGridDouble(coord1.y)
        val col2 = re.mapXToGridDouble(coord2.x)
        val row2 = re.mapYToGridDouble(coord2.y)

        val segment =
          if (row1 < row2) (col1, row1, col2, row2)
          else (col2, row2, col1, row1)

        rtree.insert(new Envelope(min(col1, col2), max(col1, col2), segment._2, segment._4), segment)
      }
    }
    rtree
  }

  /**
   * Given a list of edges, a y-value (the scanline), and a maximum
   * x-coordinate, this function generates a list of left- and
   * right-endpoints for runs of pixels.  When this function is run
   * over all of the rows, the collective output is a rasterized
   * polygon.  This implements part of the traditional scanline
   * algorithm.
   *
   * This routine ASSUMES that the polygon is closed, is of finite
   * area, and that its boundary does not self-intersect.
   *
   * @param edges  A list of active edges
   * @param y      The y-value of the vertical scanline
   * @param maxX   The maximum-possible x-coordinate
   */
  private def runsInterior(rtree : STRtree, y : Int, maxX : Int) = {
    val row = y + 0.5
    val xcoordsMap = mutable.Map[Double, Int]()
    val xcoordsList = mutable.ListBuffer[Double]()

    rtree.query(new Envelope(Double.MinValue, Double.MaxValue, row, row)).asScala.foreach({ edgeObj =>
      val edge = edgeObj.asInstanceOf[Segment]
      if (edge._2 != edge._4) { // If edge is not horizontal, process it ...
        val (xcoord, valence) = lineAxisIntersection(edge,row)
        if (xcoordsMap.contains(xcoord)) xcoordsMap(xcoord) += valence
        else xcoordsMap(xcoord) = valence
      }
    })

    xcoordsMap.foreach({ case (xcoord, valence) =>
      /* This is where the  ASSUMPTION is used.  Given the assumption,
       * this intersection  should be used as  the open or close  of a
       * run of  turned-on pixels  if and only  if the  sum associated
       * with that intersection is -1, 0, or 1. */
      if (valence == -1 || valence == 0 || valence == 1) xcoordsList += (xcoord + 0.5)
    })

    val xcoords = xcoordsList.toArray
    java.util.Arrays.sort(xcoords)
    xcoords
  }

  /**
   * This does much the same things as runsInterior, except that
   * instead of using a scanline, a "scan rectangle" is used.  When
   * this is run over all of the rows, the collective output is
   * collection of pixels which completely covers the input polygon.
   *
   * This routine ASSUMES that the polygon is closed, is of finite
   * area, and that its boundary does not self-intersect.
   *
   * @param edges  A list of active edges
   * @param y      The y-value of the bottom of the vertical scan rectangle
   * @param maxX   The maximum-possible x-coordinate
   */
  private def runsExterior(rtree : STRtree, y : Int, maxX : Int) = {
    val (top, bot) = (y + 1, y + 0)
    val interactions = mutable.ListBuffer[Segment]()
    val intervals = mutable.ListBuffer[(Double, Double)]()

    var botInterval = false
    var topInterval = false
    var botIntervalStart = 0.0
    var topIntervalStart = 0.0

    rtree.query(new Envelope(Double.MinValue, Double.MaxValue, bot, top))
      .asScala
      .foreach({ edgeObj => interactions += edgeObj.asInstanceOf[Segment] })

    interactions
      .sortWith({ (edge0, edge1) => min(edge0._1, edge0._3) < min(edge1._1, edge1._3) })
      .foreach({ edge =>

        /* Create top intervals: Generate  the list of intervals which
         * are due to  intersections of the polygon  boundary with the
         * top  of  the  scan  rectangle.   The  correctness  of  this
         * approach comes from the ASSUMPTION stated above. */
        val touchesTop = (lineAxisIntersection(edge, top)._1 != Double.NegativeInfinity)
        if (touchesTop) {
          if (topInterval == false) { // Start new top interval
            topInterval = true
            topIntervalStart = math.floor(min(edge._1, edge._3))
          }
          else if (topInterval == true) { // Finish current top interval
            topInterval = false
            intervals += ((topIntervalStart, math.ceil(max(edge._1, edge._3))))
          }
        }

        /* Create bottom intervals */
        val touchesBot = (lineAxisIntersection(edge, bot)._1 != Double.NegativeInfinity)
        if (touchesBot) {
          if (botInterval == false) { // Start new bottom interval
            botInterval = true
            botIntervalStart = math.floor(min(edge._1, edge._3))
          }
          else if (botInterval == true) { // Finish current bottom interval
            botInterval = false
            intervals += ((botIntervalStart, math.ceil(max(edge._1, edge._3))))
          }
        }

        /* Create middle intervals.  These result from boundary segments
         * entirely contained in the scan rectangle. */
        if (!touchesTop && !touchesBot) {
          intervals += ((math.floor(min(edge._1, edge._3)), math.ceil(max(edge._1, edge._3))))
        }
      })

    val sortedIntervals = intervals.sortWith({ (one, two) => one._1 < two._1 })

    /* Merge intervals */
    val mergedIntervals =
    if (sortedIntervals.length > 0) {
      val stack = mutable.Stack(sortedIntervals.head)
      sortedIntervals.tail.foreach({ interval => {
        val (l1,r1) = (stack.top._1, stack.top._2)
        val (l2,r2) = (interval._1, interval._2)
        if (r1 < l2) stack.push(interval)
        else {
          stack.pop
          stack.push((l1, max(r1,r2)))
        }
      }})
      stack.toList
    } else List.empty

    mergedIntervals.flatMap({ i => List(i._1, i._2)}).toArray
  }

  /**
   * Take a polygon, a raster extent, and a flag (described below) and
   * generate a rasterized polygon.
   *
   * @param poly             A polygon to rasterize
   * @param re               A raster extent to rasterize the polygon into
   * @param includeExterior  If false, use traditional scanline algorithm.  If true, make sure that the polygon is completely covered by pixels.
   */
  def foreachCellByPolygon(poly: Polygon, re: RasterExtent, includeExterior: Boolean = false)(f: Callback): Unit = {
    val edges = polygonToEdges(poly, re)

    var y = 0
    while(y < re.rows) {
      val rowRuns = if (includeExterior) runsExterior(edges, y, re.cols); else runsInterior(edges, y, re.cols)

      var i = 0
      while (i < rowRuns.length) {
        var x = max(rowRuns(i).toInt, 0)
        val stop = min(rowRuns(i+1).toInt, re.cols)
        while (x < stop) {
          f(x, y)
          x += 1
        } // x loop
        i += 2
      } // i loop
      y += 1
    } // y loop
  }

}
