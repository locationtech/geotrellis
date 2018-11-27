/*
 * Copyright 2018 Azavea
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

package geotrellis.vector.voronoi

import org.locationtech.jts.geom.Coordinate
import org.apache.commons.math3.linear._
import geotrellis.vector._
import geotrellis.vector.mesh.HalfEdgeTable
import geotrellis.vector.triangulation._

import scala.collection.mutable.{ListBuffer, Map}
import scala.math.{abs, max, sqrt}
import spire.syntax.cfor._

object VoronoiDiagram {

  def apply(pts: Array[Coordinate], extent: Extent, debug: Boolean = false) = {
    val dt = DelaunayTriangulation(pts, debug=debug)
    new VoronoiDiagram(dt, extent)
  }

  object V2 {
    def apply(x: Double, y: Double) = { new V2(MatrixUtils.createRealVector(Array(x, y))) }
    def apply(c: Coordinate) = { new V2(MatrixUtils.createRealVector(Array(c.x, c.y))) }
  }
  case class V2 (v: RealVector) {
    def -(that: V2) = V2(v subtract that.v)
    def +(that: V2) = V2(v add that.v)
    def +(that: Coordinate) = new Coordinate(that.x + x, that.y + y)
    def *(s: Double) = V2(v mapMultiply s)
    def dot(that: V2): Double = v dotProduct that.v
    def length() = { sqrt(v dotProduct v) }
    def normalize() = {
      val len2 = this dot this
      if (abs(len2) > 1e-16)
        this * (1/sqrt(len2))
      else
        V2(0,0)
    }
    def x() = v.getEntry(0)
    def y() = v.getEntry(1)
    override def toString() = { s"($x,$y)" }
    def toCoord() = new Coordinate(x, y)
    def rot90CCW() = V2(-y, x)
    def rot90CW() = V2(y, -x)
  }

  sealed trait CellBound
  case class BoundedRay(base: Coordinate, dir: V2) extends CellBound
  case class Ray(base: Coordinate, dir: V2) extends CellBound
  case class ReverseRay(base: Coordinate, dir: V2) extends CellBound

  private final val EPSILON = 1e-10

  private def cellBoundsNew(het: HalfEdgeTable, verts: Int => Coordinate, extent: Extent)(incidentEdge: Int): Seq[CellBound] = {
    import het._

    var e = incidentEdge
    val origin = V2(verts(getDest(e)))
    val l = collection.mutable.ListBuffer.empty[CellBound]
    do {
      val vplus = V2(verts(getSrc(rotCCWDest(e)))) - origin
      val v = V2(verts(getSrc(e))) - origin
      val vminus = V2(verts(getSrc(rotCWDest(e)))) - origin

      val xplus = origin + vplus * 0.5
      val x = origin + v * 0.5
      val xminus = origin + vminus * 0.5
      val norm = v.rot90CCW.normalize

      val aplus = - ((x - xplus).dot(vplus)) / (norm dot vplus)
      val aminus = - ((x - xminus).dot(vminus)) / (norm dot vminus)

      if (abs(norm dot vplus) < EPSILON) {
        if (abs(norm dot vminus) < EPSILON) {
          // Linear triangulation; corresponding cell edge is an infinite line
          l ++= Seq(/*ReverseRay(x.toCoord, norm * (-1)), */Ray(x.toCoord, norm))
        } else {
          // On boundary; next "face center" is point at infinity
          l += Ray((x + norm * aminus).toCoord, norm)
        }
      } else if (abs(norm dot vminus) < EPSILON) {
        // On boundary; previous "face center" is point at infinity
        ReverseRay((x + norm * aplus).toCoord, norm * (-1))
      } else if (abs(aplus - aminus) > EPSILON) {
        if (aplus > aminus) {
          // "Normal case"; cell bound is line segment
          l += BoundedRay((x + norm * aminus).toCoord, norm * (aplus - aminus))
        } else {
          if (RobustPredicates.isCCW(x.x, x.y, origin.x, origin.y, xminus.x, xminus.y)) {
            // On boundary; next "face center" is point at infinity
            l += Ray((x + norm * aminus).toCoord, norm)
          } else {
            // On boundary; previous "face center" is point at infinity
            l += ReverseRay((x + norm * aplus).toCoord, norm * (-1))
          }
        }
      } else {
        // equal alphas => degenerate line segment (equal endpoints); skip
      }

      e = rotCCWDest(e)
    } while (e != incidentEdge)
    l
  }

  private def cellExtentIntersection(het: HalfEdgeTable, verts: Int => Coordinate, incidentEdge: Int)(cell: Seq[CellBound], extent: Extent) = {
    val Extent(xmin, ymin, xmax, ymax) = extent
    val expts = ListBuffer((xmin, ymin), (xmax, ymin), (xmax, ymax), (xmin, ymax)).map{ case (x, y) => new Coordinate(x, y) }

    def clipToCellBound(poly: ListBuffer[Coordinate], bound: CellBound): ListBuffer[Coordinate] = {
      val n = poly.length
      val result = ListBuffer.empty[Coordinate]

      // println(s"current corners: ${poly}")

      def isContained(c: Coordinate) = {
        val (base, dir) = bound match {
          case BoundedRay(base, dir) => (base, dir)
          case Ray(base, dir) => (base, dir)
          case ReverseRay(base, dir) => (base, dir * (-1))
        }

        ! RobustPredicates.isCCW(base.x + dir.x, base.y + dir.y, base.x, base.y, c.x, c.y)
      }

      def crossing(ca: Coordinate, cb: Coordinate) = {
        val (base, dir) = bound match {
          case BoundedRay(base, dir) => (V2(base), dir)
          case Ray(base, dir) => (V2(base), dir)
          case ReverseRay(base, dir) => (V2(base), dir)
        }
        val norm = dir.rot90CCW
        val a = V2(ca)
        val v = V2(cb) - a
        val alpha = (base - a).dot(norm) / (v dot norm)

        (a + v * alpha).toCoord
      }

      val contained = poly.map(isContained(_))

      cfor(0)(_ < n, _ + 1) { i =>
        val v3 = poly(i)
        val v4 = poly((i + 1) % n)

        // println(s"  edge from $v3 to $v4")

        (contained(i), contained((i + 1) % n)) match {
          case (true, true)   =>
            // println("case 1")
            result += v3
          case (true, false)  =>
            // println("case 2")
            result ++= Seq(v3, crossing(v3, v4))
          case (false, true)  =>
            // println("case 3")
            result += crossing(v3, v4)
          case (false, false) =>
            // println("case 4")
            ()
        }
      }

      result
    }

    val clippedCorners = cell.foldLeft(expts)(clipToCellBound)

    if (clippedCorners isEmpty) {
      None
    } else {
      clippedCorners += clippedCorners.head
      val poly = Polygon(clippedCorners.map(Point.jtsCoord2Point(_)))
      Some(poly)
    }
  }

  def polygonalCell(het: HalfEdgeTable, verts: Int => Coordinate, extent: Extent)(incidentEdge: Int): Option[Polygon] = {
    val cell = cellBoundsNew(het, verts, extent)(incidentEdge)
    cellExtentIntersection(het, verts, incidentEdge)(cell, extent)
  }
}

/**
 * A class to compute the Voronoi diagram of a set of points.  See
 * <geotrellis_home>/docs/vector/voronoi.md for more information.
 */
class VoronoiDiagram(val dt: DelaunayTriangulation, val extent: Extent) extends Serializable {
  import VoronoiDiagram._

  val pointSet = dt.pointSet
  // private val boundEs = collection.mutable.Set.empty[Int]
  // dt.halfEdgeTable.foreachInLoop(dt.boundary){ e => boundEs += e }

  /**
   * A method to generate the Voronoi cell corresponding to the point in verts(i).  Note that if
   * verts(i) is not distinct, this function may raise an exception.
   */
  def voronoiCell(i: Int): Option[Polygon] = {
    if (dt.liveVertices.size == 1) {
      if (dt.liveVertices(i)) {
        Some(extent.toPolygon)
      } else
        throw new IllegalArgumentException(s"Cannot build Voronoi cell for nonexistent vertex $i")
    } else
      polygonalCell(dt.halfEdgeTable, pointSet.getCoordinate(_), extent)(dt.halfEdgeTable.edgeIncidentTo(i))
  }

  /**
   * The polygonal regions of the Voronoi diagram.  There exists one such convex polygon for each
   * distinct vector of verts.
   */
  def voronoiCells(): Seq[Polygon] = {
    dt.liveVertices.toSeq.flatMap(voronoiCell(_))
  }

  /**
   * Provides an iterator over the Voronoi cells of the diagram and the points that defined the
   * corresponding polygonal regions.
   */
  def voronoiCellsWithPoints(): Seq[(Polygon, Coordinate)] = {
    dt.liveVertices.toSeq.flatMap{ i: Int =>
      voronoiCell(i) match {
        case None => None
        case Some(poly) => Some(poly, dt.pointSet.getCoordinate(i))
      }
    }
  }

}
