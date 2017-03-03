/*
 * Copyright 2017 Azavea
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

package geotrellis.raster.viewshed

import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.vector._

import java.util.Arrays.binarySearch
import java.util.Comparator


object R2Viewshed extends Serializable {

  sealed abstract class From()
  case class FromNorth() extends From
  case class FromEast() extends From
  case class FromSouth() extends From
  case class FromWest() extends From
  case class FromInside() extends From

  sealed case class DirectedSegment(x0: Int, y0: Int, x1: Int, y1: Int, theta: Double) {
    override def toString(): String =
      s"($x0, $y0) to ($x1, $y1) θ=$theta"
  }

  sealed case class Ray(theta: Double, alpha: Double) {
    override def toString(): String =
      s"θ=$theta α=$alpha"
  }

  type EdgeCallback = ((Ray, From) => Unit)

  object RayComparator extends Comparator[Ray] {
    def compare(left: Ray, right: Ray): Int =
      if (left.theta < right.theta) -1
      else if (left.theta > right.theta) +1
      else 0
  }

  def generateEmptyViewshedTile(cols: Int, rows: Int) =
    ArrayTile.empty(IntCellType, cols, rows)

  /**
    * Compute the viewshed of the tile.
    */
  def apply(elevationTile: Tile, col: Int, row: Int): Tile = {
    val cols = elevationTile.cols
    val rows = elevationTile.rows
    val viewHeight = elevationTile.getDouble(col, row)
    val viewshedTile = generateEmptyViewshedTile(cols, rows)

    R2Viewshed.compute(
      elevationTile, viewshedTile,
      col, row, viewHeight, 1.0,
      FromInside(),
      null,
      { (_, _) => }
    )
    viewshedTile
  }

  /**
    * Compute the viewshed of the tile using the R2 algorithm from [1].
    *
    * 1. Franklin, Wm Randolph, and Clark Ray.
    *    "Higher isn’t necessarily better: Visibility algorithms and experiments."
    *    Advances in GIS research: sixth international symposium on spatial data handling. Vol. 2.
    *    Taylor & Francis Edinburgh, 1994.
    */
  def compute(
    elevationTile: Tile, viewshedTile: MutableArrayTile,
    startCol: Int, startRow: Int, viewHeight: Double, resolution: Double,
    from: From,
    rays: Array[Ray],
    edgeCallback: EdgeCallback,
    debug: Boolean = false
  ): Tile = {
    val cols = elevationTile.cols
    val rows = elevationTile.rows
    val re = RasterExtent(Extent(0, 0, cols, rows), cols, rows)
    val inTile: Boolean = (0 <= startCol && startCol < cols && 0 <= startRow && startRow <= rows)
    var m: Double = 0.0
    var alpha: Double = 0.0

    def computeTheta(x0: Int, y0: Int, x1: Int, y1: Int): Double = {
      val m = (y0 - y1).toDouble / (x0 - x1)

      if (x0 == x1 && y0 < y1) Math.PI/2
      else if (x0 == x1 /*&& y0 > y1*/) 1.5*Math.PI
      // else if (x0 == x1 && y0 == y1) throw new Exception
      else {
        val theta = math.atan(m)

        if (x1 >= x0 && y1 >= y0 && 0 <= theta && theta <= Math.PI/2) theta
        else if (x1 >= x0 && y1 >= y0) throw new Exception
        else if (x1 >= x0 && y1 <= y0 && -Math.PI/2 <= theta && theta <= 0) theta + 2.0*Math.PI
        else if (x1 >= x0 && y1 <= y0) throw new Exception
        else if (x1 <= x0 && y1 <= y0 && 0 <= theta && theta <= Math.PI/2) theta + Math.PI
        else if (x1 <= x0 && y1 <= y0) throw new Exception
        else if (x1 <= x0 && y1 >= y0 && -Math.PI/2 <= theta && theta <= 0) theta + Math.PI
        else if (x1 <= x0 && y1 >= y0) throw new Exception
        else throw new Exception
      }
    }

    def thetaToAlpha(theta: Double): Double = {
      from match {
        case _: FromInside => -Math.PI
        case _ =>
          val index = binarySearch(rays, Ray(theta, Double.NaN), RayComparator)
          if (index >= 0) rays(index).alpha
          else {
            val place = -1 - index
            if (place == rays.length) rays.last.alpha
            else if (place == 0) rays.head.alpha
            else if (math.abs(rays(place-1).theta - theta) < math.abs(rays(place).theta - theta))
              rays(place-1).alpha
            else rays(place).alpha
          }
      }
    }

    def clipAndQualifyRay(x0: Int, y0: Int, x1: Int, y1: Int): Option[DirectedSegment] = {
      val theta = computeTheta(x0, y0, x1, y1)
      val m = (y0 - y1).toDouble / (x0 - x1)

      from match {
        case _: FromInside if inTile => Some(DirectedSegment(x0, y0, x1, y1, theta))
        case _: FromInside if !inTile => throw new Exception
        case _: FromNorth =>
          val y2 = rows-1
          val x2 = math.round(((y2 - y1) / m) + x1).toInt
          if ((0 <= x2 && x2 < cols /*&& !(x2 == x1 && y2 == y1)*/) && (y2 <= y0 && -math.sin(theta) > 0)) {
            if (debug) println(s"BBB NORTH YES ${DirectedSegment(x2,y2,x1,y1,theta)} ${thetaToAlpha(theta)}")
            Some(DirectedSegment(x2,y2,x1,y1,theta))
          }
          else {
            if (debug) println(s"BBB NORTH NO  ($x0,$y0) ($x2,$y2) ($x1,$y1) $theta | ${0 <= x2} ${x2 < cols} ${!(x2 == x1 && y2 == y1)} ${y2 <= y0} ${-math.sin(theta) > 0}")
            None
          }
        case _: FromEast =>
          val x2 = cols-1
          val y2 = math.round((m * (x2 - x1)) + y1).toInt
          if ((0 <= y2 && y2 < rows /*&& !(x2 == x1 && y2 == y1)*/) && (x2 <= x0 && -math.cos(theta) > 0)) {
            if (debug) println(s"BBB EAST YES ${DirectedSegment(x2,y2,x1,y1,theta)}")
            Some(DirectedSegment(x2,y2,x1,y1,theta))
          }
          else {
            if (debug) println(s"BBB EAST NO  ($x0,$y0) ($x2,$y2) ($x1,$y1) $theta | ${0 <= y2} ${y2 < rows} ${!(x2 == x1 && y2 == y1)} ${x2 <= x0} ${-math.cos(theta) > 0}")
            None
          }
        case _: FromSouth =>
          val y2 = 0
          val x2 = math.round(((y2 - y1) / m) + x1).toInt
          if ((0 <= x2 && x2 < cols /*&& !(x2 == x1 && y2 == y1)*/) && (y2 >= y0 && math.sin(theta) > 0)) {
            if (debug) println(s"BBB SOUTH YES ${DirectedSegment(x2,y2,x1,y1,theta)}")
            Some(DirectedSegment(x2,y2,x1,y1,theta))
          }
          else {
            if (debug) println(s"BBB SOUTH NO  ($x0,$y0) ($x2,$y2) ($x1,$y1) $theta | ${0 <= x2} ${x2 < cols} ${!(x2 == x1 && y2 == y1)} ${y2 >= y0} ${math.sin(theta) > 0}")
            None
          }
        case _: FromWest =>
          val x2 = 0
          val y2 = math.round((m * (x2 - x1)) + y1).toInt
          if ((0 <= y2 && y2 < rows /*&& !(x2 == x1 && y2 == y1)*/) && (x2 >= x0 && math.cos(theta) > 0)) {
            if (debug) println(s"BBB WEST YES ${DirectedSegment(x2,y2,x1,y1,theta)}")
            Some(DirectedSegment(x2,y2,x1,y1,theta))
          }
          else {
            if (debug) println(s"BBB WEST NO  ($x0,$y0) ($x2,$y2) ($x1,$y1) $theta | ${0 <= y2} ${y2 < rows} ${!(x2 == x1 && y2 == y1)} ${x2 >= x0} ${math.cos(theta) > 0}")
            None
          }
      }
    }

    def callback(col: Int, row: Int) = {
      if (col == startCol && row == startRow) { // starting point
        viewshedTile.setDouble(col, row, 1)
      }
      else { // any other point
        val deltax = startCol-col
        val deltay = startRow-row
        val distance = math.sqrt(deltax*deltax + deltay*deltay) * resolution
        val angle = math.atan((elevationTile.getDouble(col, row) - viewHeight) / distance)

        if (debug) println(s"AAA $startCol $startRow col=$col row=$row ∠=$angle α=$alpha ${alpha <= angle}")
        if (alpha <= angle) {
          alpha = angle
          viewshedTile.setDouble(col, row, 1)
        }
      }
    }

    if (debug) println("NORTH")
    Range(0, cols) // North
      .flatMap({ col => clipAndQualifyRay(startCol,startRow,col,rows-1) })
      .foreach({ seg =>
        alpha = thetaToAlpha(seg.theta)
        Rasterizer.foreachCellInGridLine(
          seg.x0, seg.y0, seg.x1, seg.y1,
          null, re, false
        )(callback)
        edgeCallback(Ray(seg.theta, alpha), FromSouth()) })

    if (debug) println("EAST")
    Range(0, rows) // East
      .flatMap({ row => clipAndQualifyRay(startCol,startRow,cols-1,row) })
      .foreach({ seg =>
        alpha = thetaToAlpha(seg.theta)
        Rasterizer.foreachCellInGridLine(
          seg.x0, seg.y0, seg.x1, seg.y1,
          null, re, false
        )(callback)
        edgeCallback(Ray(seg.theta, alpha), FromWest()) })

    if (debug) println("SOUTH")
    Range(0, cols) // South
      .flatMap({ col => clipAndQualifyRay(startCol,startRow,col,0) })
      .foreach({ seg =>
        alpha = thetaToAlpha(seg.theta)
        Rasterizer.foreachCellInGridLine(
          seg.x0, seg.y0, seg.x1, seg.y1,
          null, re, false
        )(callback)
        edgeCallback(Ray(seg.theta, alpha), FromNorth()) })

    if (debug) println("WEST")
    Range(0, rows) // West
      .flatMap({ row => clipAndQualifyRay(startCol,startRow,0,row) })
      .foreach({ seg =>
        alpha = thetaToAlpha(seg.theta)
        Rasterizer.foreachCellInGridLine(
          seg.x0, seg.y0, seg.x1, seg.y1,
          null, re, false
        )(callback)
        edgeCallback(Ray(seg.theta, alpha), FromEast()) })

    viewshedTile
  }

}
