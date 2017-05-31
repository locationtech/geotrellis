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

import scala.collection.mutable

import java.util.Arrays.binarySearch
import java.util.Comparator


object R2Viewshed extends Serializable {

  sealed abstract class From()
  case class FromNorth() extends From
  case class FromEast() extends From
  case class FromSouth() extends From
  case class FromWest() extends From
  case class FromInside() extends From

  sealed abstract class AggregationOperator()
  case class And() extends AggregationOperator { override def toString: String = "AND" }
  case class Debug() extends AggregationOperator { override def toString: String = "DEBUG" }
  case class Or() extends AggregationOperator { override def toString: String = "OR" }

  sealed case class DirectedSegment(x0: Int, y0: Int, x1: Int, y1: Int, theta: Double) {
    override def toString(): String = s"($x0, $y0) to ($x1, $y1) θ=$theta"
    def isRumpSegment(): Boolean = ((x0 == x1) && (y0 == y1))
    def isNearVertical(epsilon: Double): Boolean = {
      ((math.abs(theta - 0.5*math.Pi) < epsilon) ||
       (math.abs(theta - 1.5*math.Pi) < epsilon))
    }
    def isNearHorizontal(epsilon: Double): Boolean = {
      ((math.abs(theta - 0.0*math.Pi) < epsilon) ||
       (math.abs(theta - 1.0*math.Pi) < epsilon) ||
       (math.abs(theta - 2.0*math.Pi) < epsilon))
    }
  }

  sealed case class Ray(theta: Double, alpha: Double) {
    override def toString(): String = s"θ=$theta α=$alpha"
  }

  type Bundle = Map[From, mutable.ArrayBuffer[Ray]]
  type TileCallback = (Bundle => Unit)
  def nop(b: Bundle): Unit = {}

  /**
    * A Comparator for Rays which compares them by their theta angle.
    * This is used in the binary search that is performed in
    * thetaToAlpha.
    */
  object RayComparator extends Comparator[Ray] {
    def compare(left: Ray, right: Ray): Int =
      if (left.theta < right.theta) -1
      else if (left.theta > right.theta) +1
      else 0
  }

  /**
    * Generate an empty viewshed tile.
    *
    * @param  cols  The number of columns
    * @param  rows  The number of rows
    */
  def generateEmptyViewshedTile(cols: Int, rows: Int) =
    ArrayTile.empty(IntConstantNoDataCellType, cols, rows)

  /**
    * Given a direction of propagation, a packet of rays, and an angle
    * theta, return alpha (the angle of elevation).
    */
  private def thetaToAlpha(from: From, rays: Array[Ray], theta: Double): Double = {
    from match {
      case _: FromInside => -math.Pi
      case _ =>
        val index = binarySearch(rays, Ray(theta, Double.NaN), RayComparator)

        if (index >= 0) rays(index).alpha
        else {
          val place = -1 - index

          if (place == rays.length) {
            if (math.abs(rays.last.theta - theta) < math.abs(rays.head.theta - theta - 2*math.Pi))
              rays.last.alpha
            else
              rays.head.alpha
          }
          else if (place == 0) {
            if (math.abs(rays.head.theta - theta) < math.abs(rays.last.theta - theta + 2*math.Pi))
              rays.head.alpha
            else
              rays.last.alpha
          }
          else if (math.abs(rays(place-1).theta - theta) < math.abs(rays(place).theta - theta)) // left
            rays(place-1).alpha
          else // right
            rays(place).alpha
        }
    }
  }

  /**
    * Compute the drop in elevation due to Earth's curvature (please
    * see [1]).
    *
    * 1. https://en.wikipedia.org/wiki/Arc_(geometry)
    */
  @inline private def downwardCurvature(distance: Double): Double =
    6378137 * (1 - math.cos(distance / 6378137))

  /**
    * Compute the viewshed of the tile using the R2 algorithm.  Makes
    * use of the compute method of this object.
    *
    * @param  elevationTile  Elevations in units of meters
    * @param  startCol       The x position of the vantage point
    * @param  startRow       The y position of the vantage point
    */
  def apply(
    elevationTile: Tile,
    startCol: Int, startRow: Int,
    op: AggregationOperator = Or()): Tile = {
    val cols = elevationTile.cols
    val rows = elevationTile.rows
    val viewHeight = elevationTile.getDouble(startCol, startRow)
    val viewshedTile =
      op match {
        case _: Or => ArrayTile.empty(IntCellType, cols, rows)
        case _ => ArrayTile.empty(IntConstantNoDataCellType, cols, rows)
      }

    R2Viewshed.compute(
      elevationTile, viewshedTile,
      startCol, startRow, viewHeight,
      FromInside(),
      null,
      nop,
      resolution = 1,
      maxDistance = Double.PositiveInfinity,
      curvature = false,
      operator = op,
      altitude = Double.NegativeInfinity,
      cameraDirection = 0,
      cameraFOV = -1.0
    )
    viewshedTile
  }

  /**
    * Compute the viewshed of the elevatonTile using the R2 algorithm
    * from [1].  The numbers in the elevationTile are assumed to be
    * elevations in units of meters.  The results are written into the
    * viewshedTile.
    *
    * 1. Franklin, Wm Randolph, and Clark Ray.
    *    "Higher isn’t necessarily better: Visibility algorithms and experiments."
    *    Advances in GIS research: sixth international symposium on spatial data handling. Vol. 2.
    *    Taylor & Francis Edinburgh, 1994.
    *
    *
    * @param  elevationTile    Elevations in units of meters
    * @param  viewshedTile     The tile into which the viewshed will be written
    * @param  startCol         The x position of the vantage point
    * @param  startRow         The y position of the vantage point
    * @param  viewHeight       The absolute height (above sea level) of the vantage point
    * @param  from             The direction from which the rays are allowed to come
    * @param  rays             Rays shining in from other tiles
    * @param  tileCallback     A callback to communicate rays which have reached the periphery of the tile
    * @param  resolution       The resolution of the elevationTile in units of meters/pixel
    * @param  maxDistance      The maximum distance that any ray is allowed to travel
    * @param  curvature        Whether to account for the Earth's curvature or not
    * @param  operator         The aggregation operator to use
    * @param  altitude         The absolute altitude (above sea level) to query; if -∞, use the terrain height
    * @param  cameraDirection  The direction (in radians) of the camera
    * @param  cameraFOV        The camera field of view, rays whose dot product with the camera direction are less than this are filtered out
    */
  def compute(
    elevationTile: Tile, viewshedTile: MutableArrayTile,
    startCol: Int, startRow: Int, viewHeight: Double,
    from: From,
    rays: Array[Ray],
    tileCallback: TileCallback,
    resolution: Double,
    maxDistance: Double,
    curvature: Boolean,
    operator: AggregationOperator,
    altitude: Double = Double.NegativeInfinity,
    cameraDirection: Double = 0,
    cameraFOV: Double = -1.0,
    epsilon: Double = (1/math.Pi),
    debugCol: Int = -1,
    debugRow: Int = -1
  ): Tile = {
    val cols = elevationTile.cols
    val rows = elevationTile.rows
    val re = RasterExtent(Extent(0, 0, cols, rows), cols, rows)
    val inTile: Boolean = (0 <= startCol && startCol < cols && 0 <= startRow && startRow <= rows)
    val vx = math.cos(cameraDirection)
    val vy = math.sin(cameraDirection)

    def clipAndQualifyRay(from: From)(x0: Int, y0: Int, x1: Int, y1: Int): Option[DirectedSegment] = {
      val _theta = math.atan2((y1-y0), (x1-x0))
      val theta = if (_theta >= 0.0) _theta ; else _theta + 2*math.Pi
      val m = (y0 - y1).toDouble / (x0 - x1)

      from match {
        case _ if ((-1.0 < cameraFOV && cameraFOV < 1.0) && (vx*math.cos(theta) + vy*math.sin(theta)) < cameraFOV) =>
          None
        case _: FromInside if inTile => Some(DirectedSegment(x0,y0,x1,y1,theta))
        case _: FromInside if !inTile => throw new Exception
        case _: FromNorth =>
          val y2 = rows-1
          val x2 = math.round(((y2 - y1) / m) + x1).toInt
          if ((0 <= x2 && x2 < cols) && (y2 <= y0 && -math.sin(theta) > 0.0))
            Some(DirectedSegment(x2,y2,x1,y1,theta))
          else None
        case _: FromEast =>
          val x2 = cols-1
          val y2 = math.round((m * (x2 - x1)) + y1).toInt
          if ((0 <= y2 && y2 < rows) && (x2 <= x0 && -math.cos(theta) > 0.0))
            Some(DirectedSegment(x2,y2,x1,y1,theta))
          else None
        case _: FromSouth =>
          val y2 = 0
          val x2 = math.round(((y2 - y1) / m) + x1).toInt
          if ((0 <= x2 && x2 < cols) && (y2 >= y0 && math.sin(theta) > 0.0))
            Some(DirectedSegment(x2,y2,x1,y1,theta))
          else None
        case _: FromWest =>
          val x2 = 0
          val y2 = math.round((m * (x2 - x1)) + y1).toInt
          if ((0 <= y2 && y2 < rows) && (x2 >= x0 && math.cos(theta) > 0.0))
            Some(DirectedSegment(x2,y2,x1,y1,theta))
          else None
      }
    }

    var alpha: Double = Double.NaN
    var terminated: Boolean = false

    def callback(col: Int, row: Int) = {
      if (col == startCol && row == startRow) { // starting point
        viewshedTile.setDouble(col, row, 1)
      }
      else if (!terminated) { // any other point
        val deltax = startCol - col
        val deltay = startRow - row
        val distance = math.sqrt(deltax * deltax + deltay * deltay) * resolution
        val drop = if (curvature) downwardCurvature(distance); else 0.0
        val elevation = elevationTile.getDouble(col, row) - drop - viewHeight
        val groundAngle = math.atan(elevation / distance)
        val angle =
          if (altitude == Double.NegativeInfinity) groundAngle
          else  math.atan((altitude - drop - viewHeight) / distance)

        if (distance >= maxDistance) terminated = true
        if (!terminated) {
          val groundVisible = alpha <= groundAngle
          val visible = alpha <= angle
          val current = viewshedTile.get(col, row)
          val colrow = (col, row)

          if (groundVisible) alpha = groundAngle

          operator match {
            case _: Or if visible =>
              viewshedTile.set(col, row, 1)
            case _: And if !visible =>
              viewshedTile.set(col, row, 0)
            case _: And if (visible && isNoData(current)) =>
              viewshedTile.set(col, row, 1)
            case _: Debug if (visible && isNoData(current)) =>
              viewshedTile.set(col, row, 1)
            case _: Debug if (visible && !isNoData(current)) =>
              viewshedTile.set(col, row, 1 + current)
            case _ =>
          }
        }
      }
    }

    val clip = clipAndQualifyRay(from)_
    val northRays = mutable.ArrayBuffer.empty[Ray]
    val eastRays = mutable.ArrayBuffer.empty[Ray]
    val southRays = mutable.ArrayBuffer.empty[Ray]
    val westRays = mutable.ArrayBuffer.empty[Ray]
    val northSegs = Range(0, cols).flatMap({ col => clip(startCol,startRow,col,rows-1) })
    val southSegs = Range(0, cols).flatMap({ col => clip(startCol,startRow,col,0) })
    val eastSegs = Range(0, rows).flatMap({ row => clip(startCol,startRow,cols-1,row) })
    val westSegs = Range(0, rows).flatMap({ row => clip(startCol,startRow,0,row) })

    /***************
     * GOING NORTH *
     ***************/
    northSegs
      .foreach({ seg =>
        alpha = thetaToAlpha(from, rays, seg.theta); terminated = false
        Rasterizer.foreachCellInGridLine(seg.x0, seg.y0, seg.x1, seg.y1, null, re, false)(callback)
        if (!terminated && !seg.isRumpSegment) southRays.append(Ray(seg.theta, alpha))
      })

    /***************
     * GOING SOUTH *
     ***************/
    southSegs
      .foreach({ seg =>
        alpha = thetaToAlpha(from, rays, seg.theta); terminated = false
        Rasterizer.foreachCellInGridLine(seg.x0, seg.y0, seg.x1, seg.y1, null, re, false)(callback)
        if (!terminated && !seg.isRumpSegment) northRays.append(Ray(seg.theta, alpha))
      })

    /**************
     * GOING EAST *
     **************/
    if ((cols <= startCol) && (startCol <= 2*cols) &&
        (eastSegs.forall(_.isNearVertical(epsilon))) &&
        (eastSegs.forall(_.isRumpSegment))) { // Sharp angle case
      Range(0, rows).foreach({ row => viewshedTile.set(cols-1, row, viewshedTile.get(cols-2, row)) })
    } else { // Normal case
      eastSegs
        .foreach({ seg =>
          alpha = thetaToAlpha(from, rays, seg.theta); terminated = false
          Rasterizer.foreachCellInGridLine(seg.x0, seg.y0, seg.x1, seg.y1, null, re, false)(callback)
          if (!terminated && !seg.isRumpSegment) westRays.append(Ray(seg.theta, alpha))
        })
    }

    /**************
     * GOING WEST *
     **************/
    if ((-1*cols < startCol) && (startCol < 0) &&
        (westSegs.forall(_.isNearVertical(epsilon))) &&
        (westSegs.forall(_.isRumpSegment))) {
      Range(0, rows).foreach({ row => viewshedTile.set(0, row, viewshedTile.get(1, row)) })
    } else {
      westSegs
        .foreach({ seg =>
          alpha = thetaToAlpha(from, rays, seg.theta); terminated = false
          Rasterizer.foreachCellInGridLine(seg.x0, seg.y0, seg.x1, seg.y1, null, re, false)(callback)
          if (!terminated && !seg.isRumpSegment) eastRays.append(Ray(seg.theta, alpha))
        })
    }

    /***************
     * NORTH AGAIN *
     ***************/
    if ((rows <= startRow) && (startRow <= 2*rows) &&
        (northSegs.forall(_.isNearHorizontal(epsilon))) &&
        (northSegs.forall(_.isRumpSegment))) {
      Range(0, cols).foreach({ col => viewshedTile.set(col, rows-1, viewshedTile.get(col, rows-2)) })
    }

    /***************
     * SOUTH AGAIN *
     ***************/
    if ((-1*rows < startRow) && (startRow < 0) &&
        (southSegs.forall(_.isNearHorizontal(epsilon))) &&
        (southSegs.forall(_.isRumpSegment))) {
      Range(0, cols).foreach({ col => viewshedTile.set(col, 0, viewshedTile.get(col, 1)) })
    }

    val bundle: Bundle = Map(
      FromSouth() -> southRays,
      FromWest() -> westRays,
      FromNorth() -> northRays,
      FromEast() -> eastRays
    )
    tileCallback(bundle)

    viewshedTile
  }

}
