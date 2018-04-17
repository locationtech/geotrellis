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

package geotrellis.raster.rasterize.polygon

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.vector._

import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.index.strtree.STRtree
import spire.syntax.cfor._

import scala.collection.JavaConverters._
import math.{min, max, ceil, floor}


/**
  * Object holding polygon rasterization functions.
  */
object PolygonRasterizer {
  import scala.collection.mutable
  import java.util.{Arrays, Comparator}
  import math.{abs,min,max}

  type Segment = (Double, Double, Double, Double)
  type Interval = (Double, Double)

  private object IntervalCompare extends Comparator[Interval] {
    def compare(a: Interval, b: Interval) : Int = {
      val difference = a._1 - b._1
      if (difference < 0.0) -1
      else if (difference == 0.0) 0
      else 1
    }
  }

  private def intervalIntersection(a: Interval, b: Interval) : List[Interval] = {
    val left = max(a._1, b._1)
    val right = min(a._2, b._2)
    if (left <= right) List((left, right))
    else List.empty[Interval]
  }

  private def intervalDifference(a : Interval, b: Interval) : List[Interval] = {
    val (aLeft, aRight) = a
    val (bLeft, bRight) = b

    if (aLeft <= bLeft && bRight <= aRight) List((aLeft,bLeft), (bRight,aRight))
    else if (bLeft <= aLeft && aRight <= bRight) List.empty[Interval]
    else if (aLeft <= bLeft && bLeft <= aRight) List((aLeft,bLeft))
    else if (bLeft <= aLeft && bRight <= aRight) List((bRight,aRight))
    else List.empty[Interval]
  }

  private def intervalDifference(a : List[Interval], b: Interval) : List[Interval] = a.flatMap(intervalDifference(_,b))

  private def mergeIntervals(sortedIntervals : Seq[Interval]) : Array[Double] = {
    if (sortedIntervals.length > 0) {
      val head = sortedIntervals.head
      val stack = mutable.Stack(head._1, head._2)

      sortedIntervals.tail.foreach({ interval => {
        val (l1,r1) = (stack(0), stack(1))
        val (l2,r2) = (interval._1, interval._2)
        if (r1 < l2) {
          stack.push(interval._2)
          stack.push(interval._1)
        }
        else {
          stack.pop
          stack.pop
          stack.push(max(r1,r2))
          stack.push(l1)
        }
      }})
      stack.toArray
    } else Array.empty[Double]
  }

  /**
   * Given a Segment and a y-value (for a line parallel to the
   * x-axis), return the point of intersection and whether it occurs
   * at the top, middle, or bottom of the line segment.
   *
   * @param line  A line segment
   * @param y     The y-value for a line parallel to the x-axis
   */
  private def lineAxisIntersection(line: Segment, y: Double) = {
    val (x1, y1, x2, y2) = line

    if (y == y1) (x1,1) // Top endpoint
    else if (y == y2) (x2,-1) // Bottom endpoint
    else if ((min(y1,y2) <= y) && (y <= max(y1,y2))) { // Between endpoints
      (((x1-x2)*y-(x1*y2-y1*x2))/(y1-y2),0)
    }
    else (Double.NegativeInfinity, Int.MaxValue) // No intersection
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

    /** Find the outer ring's segments */
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

    /** Find the segments for the holes */
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
  private def runsPoint(rtree: STRtree, y: Int, maxX: Int) = {
    val row = y + 0.5
    val xcoordsMap = mutable.Map[Double, Int]()
    val xcoordsList = mutable.ListBuffer[Double]()

    val segments = {
      var nonHorizontal = 0
      var horizontal = false

      val segments =
        rtree
          .query(new Envelope(Double.MinValue, Double.MaxValue, row, row))
          .asScala
          .flatMap({ edgeObj =>
            val edge = edgeObj.asInstanceOf[Segment]
            if (edge._2 != edge._4) {
              val (xcoord, parity) = lineAxisIntersection(edge,row)
              nonHorizontal += 1
              if (parity != Int.MaxValue) Some((xcoord, parity))
              else None
            }
            else {
              horizontal = true
              None
            }
          })

      /**
        * Remove horizontal edges by perturbing the scanline up or
        * down infinitesimally.
        */
      if (horizontal == true) {
        val upward = segments.filter({ case (_, parity) => parity != 1 })
        val downward = segments.filter({ case (_, parity) => parity != -1 })
        val upwardDistance = math.abs(nonHorizontal - upward.length)
        val downwardDistance = math.abs(nonHorizontal - downward.length)

        /**
          * A better measure of distance would probably be the
          * Levenshtein distance[1], but instead the respective
          * differences in length from the original sequence (minus
          * horizontal edges) are used as proxies.
          *
          * https://en.wikipedia.org/wiki/Levenshtein_distance
          */
        if (upwardDistance < downwardDistance) upward
        else downward
      }
      else segments
    }

    segments.foreach({ case (xcoord, parity) =>
      if (xcoordsMap.contains(xcoord)) xcoordsMap(xcoord) += parity
      else xcoordsMap(xcoord) = parity
    })

    xcoordsMap.foreach({ case (xcoord, parity) =>
      /**
        * This is where the  ASSUMPTION is used.  Given the assumption,
        * this intersection  should be used as  the open or close  of a
        * run of  turned-on pixels  if and only  if the  sum associated
        * with that intersection is -1, 0, or 1.
        */
      if (parity == -1 || parity == 0 || parity == 1)
        xcoordsList += (xcoord + 0.5)
    })

    val xcoords = xcoordsList.toArray
    Arrays.sort(xcoords)
    xcoords
  }

  /**
    * This does much the same things as runsPoint, except that instead
    * of using a scanline, a "scan-rectangle" is used.  When this is
    * run over all of the rows, the collective output is collection of
    * pixels which completely covers the input polygon (when partial
    * is true) or the collection of pixels which are completely inside
    * of the polygon (when partial = false).
    *
    * This routine ASSUMES that the polygon is closed, is of finite
    * area, and that its boundary does not self-intersect.
    *
    * @param edges    A list of active edges
    * @param y        The y-value of the bottom of the vertical scan-rectangle
    * @param maxX     The maximum-possible x-coordinate
    * @param partial  True if all intersected cells are to be reported, otherwise only those on the interior of the polygon
    */
  private def runsArea(rtree: STRtree, y: Int, maxX: Int, partial: Boolean) = {
    val (top, bot) = (y + 1, y + 0)
    val interactions = mutable.ListBuffer[Segment]()
    val intervals = mutable.ListBuffer[Interval]()
    val botIntervals = mutable.ListBuffer[Interval]()
    val topIntervals = mutable.ListBuffer[Interval]()
    val midIntervals = mutable.ListBuffer[Interval]()

    var botInterval = false
    var topInterval = false
    var botIntervalStart = 0.0
    var topIntervalStart = 0.0

    /**
      * Process each edge  which intersects the scan-rectangle.  Report
      * all edges  except those which only  touch the top or  bottom of
      * the scan-rectangle.
      */
    rtree.query(new Envelope(Double.MinValue, Double.MaxValue, bot, top))
      .asScala
      .foreach({ edgeObj =>
        val edge = edgeObj.asInstanceOf[Segment]
        val minY = min(edge._2, edge._4)
        val maxY = max(edge._2, edge._4)
        val ignore = (maxY == bot || minY == top)

        if (!ignore) {
          interactions += (
            if (edge._1 <= edge._3) edge
            else (edge._3, edge._4, edge._1, edge._2)
          )}
      })

    interactions
      .sortWith(_._1 < _._1)
      .foreach({ edge =>
        val topIntervalX = lineAxisIntersection(edge, top)._1
        val botIntervalX = lineAxisIntersection(edge, bot)._1
        val touchesTop = (topIntervalX != Double.NegativeInfinity)
        val touchesBot = (botIntervalX != Double.NegativeInfinity)

        /**
          * If partial pixels are being reported, then all pixels which
          * intersect with an edge must be reported.
          */
        if (partial) {
          val firstX =
            if (edge._2 <= bot) botIntervalX
            else if (edge._2 >= top) topIntervalX
            else edge._1
          val secondX =
            if (edge._4 <= bot) botIntervalX
            else if (edge._4 >= top) topIntervalX
            else edge._3

          val smallerX = min(firstX, secondX)
          val largerX = max(firstX, secondX)

          intervals += ((floor(smallerX), ceil(largerX)))
        }

        /**
          * Create top intervals: Generate the list of intervals which
          * are due to intersections of the polygon boundary with the
          * top of the scan-rectangle.  The correctness of this
          * approach comes from the ASSUMPTION stated above.
          */
        if (touchesTop) {
          if (topInterval == false) { // Start new top interval
            topInterval = true
            topIntervalStart = topIntervalX
          }
          else if (topInterval == true) { // Finish current top interval
            topInterval = false
            val smaller = min(topIntervalStart, topIntervalX)
            val larger = max(topIntervalStart, topIntervalX)

            if (partial) intervals += ((floor(smaller), ceil(larger)))
            else topIntervals += ((ceil(smaller), floor(larger)))
          }
        }

        /** Create bottom intervals. */
        if (touchesBot) {
          if (botInterval == false) { // Start new bot interval
            botInterval = true
            botIntervalStart = botIntervalX
          }
          else if (botInterval == true) { // Finish current bot interval
            botInterval = false
            val smaller = min(botIntervalStart, botIntervalX)
            val larger = max(botIntervalStart, botIntervalX)

            if (partial) intervals += ((floor(smaller), ceil(larger)))
            else botIntervals += ((ceil(smaller), floor(larger)))
          }
        }

        /**
          * Create middle intervals.  These result form boundary
          * segments in the interior of the scan-rectangle.
          */
        if (!partial && (!touchesTop || !touchesBot))
          midIntervals += ((math.floor(edge._1), math.ceil(edge._3)))
      })

    if (partial)
      mergeIntervals(intervals.sortWith(_._1 < _._1))
    else {
      /**
        * When partial pixels are not being reported, intervals from
        * intersections with the top and bottom of the scan-rectangle
        * must ratify one-another.
        */
      val sortedTopIntervals = topIntervals.sortWith(_._1 < _._1)
      val sortedBotIntervals = botIntervals.sortWith(_._1 < _._1)
      val intervals = mutable.ListBuffer.empty[Interval]

      sortedTopIntervals.zip(sortedBotIntervals).map({ case(a,b) =>
        val intersection = intervalIntersection(a,b)
        /**
          * Thanks to the ASSUMPTION stated above, middle intervals imply
          * partial pixels in their x-extents.
          */
        val differences = midIntervals.foldLeft(intersection)(intervalDifference)
        intervals ++= differences
      })

      mergeIntervals(intervals)
    }
  }

  /**
   * This function causes the function f to be called on each pixel
   * that interacts with the polygon.  The definition of the word
   * "interacts" is controlled by the options parameter.
   *
   * @param poly     A polygon to rasterize
   * @param re       A raster extent to rasterize the polygon into
   * @param options  The options parameter controls whether to treat pixels as points or areas and whether to report partially-intersected areas.
   */
  def foreachCellByPolygon(
    poly: Polygon,
    re: RasterExtent,
    options: Options = Options.DEFAULT
  )(f: Callback): Unit = {
    val sampleType = options.sampleType
    val partial = options.includePartial

    val edges = polygonToEdges(poly, re)

    var y = 0
    while(y < re.rows) {
      val rowRuns =
        if (sampleType == PixelIsPoint) runsPoint(edges, y, re.cols)
        else runsArea(edges, y, re.cols, partial)

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
