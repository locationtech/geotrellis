package geotrellis.vector.voronoi

import geotrellis.vector._
import geotrellis.util.Constants.DOUBLE_EPSILON
import scala.collection.mutable.Map
import org.apache.commons.math3.linear._
import scala.math.{abs,sqrt}

/**
 * A class to compute the Voronoi diagram of a set of points.  See
 * <geotrellis_home>/docs/vector/voronoi.md for more information.
 */
class Voronoi(val verts: Array[Point], val extent: Extent) {
  
  object V2 {
    def apply(x: Double, y: Double) = { new V2(MatrixUtils.createRealVector(Array(x,y))) }
    def apply(p: Point) = { new V2(MatrixUtils.createRealVector(Array(p.x, p.y))) }
  }
  case class V2 (v: RealVector) {
    def -(that: V2) = V2(v subtract that.v)
    def +(that: V2) = V2(v add that.v)
    def +(that: Point) = Point(that.x + x, that.y + y)
    def *(s: Double) = V2(v mapMultiply s)
    def dot(that: V2): Double = v dotProduct that.v
    def length() = { sqrt(v dotProduct v) }
    def normalize() = {
      val len2 = this dot this
      if (abs(len2) < 1e-16)
        this * (1/sqrt(len2))
      else
        V2(0,0)
    }
    def x() = v.getEntry(0)
    def y() = v.getEntry(1)
    override def toString() = { s"($x,$y)" }
    def toPoint() = Point(x, y)
    def rot90CCW() = V2(-y, x)
    def rot90CW() = V2(y, -x)
  }

  /**
   * The dual Delaunay triangulation of the Voronoi diagram.
   */
  lazy val dt = new Delaunay(verts)

  private def rayLineIntersection(base: V2, normal: V2, a: V2, b: V2): Option[(Double,Point)] = {
    // (a,b) herein called the "edge"
    // (base, normal) herein called the "ray"

    val num = normal dot (a-base)
    val den = normal dot (a-b)
    
    if (abs(den) < 1e-16) {
      // ray and edge are parallel (THIS SHOULD NEVER HAPPEN!! (otherwise extent is not a bounding box))
      None
      /*if (abs(num) < 1e-16) {
        // edge lies on ray => base is intersection
        val x = b - a
        val y = base - a
        val r = (x dot y) / (x dot x)
        if (0 <= r && r < 1)
          Some((r, base.toPoint))
        else
          None
      } else {
        // base not on edge => no intersection
        None
      }*/
    } else {
      val t = num / den

      if (0 <= t && t < 1) {
        val pt = a + (b - a) * t
        val dir = normal.rot90CW

        if ((pt - base).dot(dir) >= 0)
          Some(((pt - base).length, pt.toPoint))
        else
          None
      } else
        None
    }
  }

  private val TOP_EDGE = 0
  private val LEFT_EDGE = 1
  private val BOTTOM_EDGE = 2
  private val RIGHT_EDGE = 3
  private val INTERIOR = 4

  sealed trait CellBound
  case class BoundedRay(base: Point, dir: V2) extends CellBound
  case class Ray(base: Point, dir: V2) extends CellBound
  case class ReverseRay(base: Point, dir: V2) extends CellBound

  private def rayExtentIntersection(base: V2, normal: V2): List[(Double, Point, Int)] = {
    def rli(a: V2, b: V2) = { rayLineIntersection(base, normal, a, b) }
    val tr = V2(extent.xmax, extent.ymax)
    val tl = V2(extent.xmin, extent.ymax)
    val bl = V2(extent.xmin, extent.ymin)
    val br = V2(extent.xmax, extent.ymin)

    List(rli(tr, tl), rli(tl, bl), rli(bl, br), rli(br, tr))
      .zip(List(TOP_EDGE, LEFT_EDGE, BOTTOM_EDGE, RIGHT_EDGE))
      .filter{ x => x._1 != None }
      .map{ case ((Some((d, x)), e)) => (d, x, e) ; case _ => throw new IllegalArgumentException("Unexpeceted None encountered") }
  }

  private def firstRayExtentIntersection(ray: CellBound): Option[(Point, Int)] = {
    def ascending(x: (Double, Point, Int), y: (Double, Point, Int)): Boolean = x._1 < y._1
    def descending(x: (Double, Point, Int), y: (Double, Point, Int)): Boolean = x._1 > y._1

    val candidates = ray match {
      case BoundedRay(base, dir) => {
        val maxlen = dir.length
        rayExtentIntersection(V2(base), dir.rot90CCW)
          .filter{ case ((d, _, _)) => d < maxlen }
          .sortWith(ascending)
      }
      case Ray(base, dir) => {
        if (extent.contains(base))
          List((0.0, base, INTERIOR))
        else
          Nil
      }
      case ReverseRay(base, dir) => rayExtentIntersection(V2(base), dir.rot90CCW).sortWith(descending)
    }

    candidates match {
      case Nil => None: Option[(Point, Int)]
      case ((_, x, e) :: _) => Some((x, e))
    }
  }

  private def lastRayExtentIntersection(ray: CellBound): Option[(Point, Int)] = {
    def ascending(x: (Double, Point, Int), y: (Double, Point, Int)): Boolean = x._1 < y._1
    def descending(x: (Double, Point, Int), y: (Double, Point, Int)): Boolean = x._1 > y._1

    val candidates = ray match {
      case BoundedRay(base, dir) => {
        val maxlen = dir.length
        if (extent.covers(dir + base)) {
          List((maxlen, dir + base, INTERIOR))
        } else {
          rayExtentIntersection(V2(base), dir.rot90CCW)
            .filter{ case ((d, _, _)) => d <= maxlen }
            .sortWith(descending)
        }
      }
      case Ray(base, dir) => rayExtentIntersection(V2(base), dir.rot90CCW).sortWith(descending)
      case ReverseRay(base, dir) => {
        if (extent.covers(base))
          List((0.0, base, INTERIOR))
        else Nil
      }
    }

    candidates match {
      case Nil => None
      case ((_, x, e) :: _) => Some((x, e))
    }
  }    

  /*
   * Function to create the components of the boundary of a Voronoi cell.
   * Finite edges are "BoundedRay" objects, infinite edges are represented as
   * either Ray or ReverseRay objects.  The directions of the rays are
   * consistent with a CCW winding of the Voronoi cells.  (ReverseRays are
   * thought of as rays originating at some point at infinity and arriving at
   * the endpoint, even though they have the same representation as a regular
   * Ray.)
   */
  private def cellBounds(incidentEdge: HalfEdge[Int, Point]): List[CellBound] = {
    var e = incidentEdge
    var l = Nil: List[CellBound]
    do {
      (e.face, e.flip.face) match {
        // Two points in "triangulation"
        case (None, None) => {
          val distal = V2(verts(e.src))
          val proximal = V2(verts(e.vert))
          val mid = ((distal + proximal) * 0.5).toPoint
          val dir = distal - proximal
          l = l ++ List(Ray(mid, dir.rot90CCW), ReverseRay(mid, dir.rot90CW))
        }
        // Boundary edge (ray to infinity)
        case (Some(c), None) => l = l :+ Ray(c, (V2(verts(e.vert)) - V2(verts(e.src))).rot90CW)
        // Boundary edge (reverse ray from infinity)
        case (None, Some(c)) => l = l :+ ReverseRay(c, (V2(verts(e.vert)) - V2(verts(e.src))).rot90CCW)
        // Normal edge
        case (Some(c), Some(d)) if (c.distance(d) > DOUBLE_EPSILON) => l = l :+ BoundedRay(c, V2(d) - V2(c))
      }
      e = e.rotCCWDest
    } while (e != incidentEdge)
    l
  }

  private def infill(fst: (Point, Int), snd: (Point, Int)): List[Point] = {
    var accum: List[Point] = Nil
    var i = fst._2
    
    while (i != snd._2) {
      i match {
        case TOP_EDGE    => accum = accum :+ Point(extent.xmin,extent.ymax)
        case LEFT_EDGE   => accum = accum :+ Point(extent.xmin,extent.ymin)
        case BOTTOM_EDGE => accum = accum :+ Point(extent.xmax,extent.ymin)
        case RIGHT_EDGE  => accum = accum :+ Point(extent.xmax,extent.ymax)
      }
      i = (i+1)%4
    }
    accum :+ snd._1
  }

  /**
   * A method to generate the Voronoi cell corresponding to the point in verts(incidentEdge.vert).
   * If the incident edge is not an interior edge (incidentEdge.face == None) the results are
   * undefined.
   */
  def voronoiCell(incidentEdge: HalfEdge[Int, Point]): Polygon = {
    val cell = cellBounds(incidentEdge)
    var i = 0
    var accum: List[Point] = Nil

    while (i < cell.length) {
      val lrei = lastRayExtentIntersection(cell(i))
      i = i + 1
      if (lrei != None) {
        val (cellVert, vertLoc) = lrei.get
        accum = accum :+ cellVert
        if (vertLoc != INTERIOR) {
          var frei = firstRayExtentIntersection(cell(i % cell.length))
          while(frei == None) {
            i = i + 1
            frei = firstRayExtentIntersection(cell(i % cell.length))
          }
          accum = accum ++ infill(lrei.get, frei.get)
        }
      }
    }

    Polygon(Line(accum).closed)
  }

  /**
   * A method to generate the Voronoi cell corresponding to the point in verts(i).  Note that if
   * verts(i) is not distinct, this function may raise an exception.
   */
  def voronoiCell(i: Int): Polygon = {
    voronoiCell(dt.faceIncidentToVertex(i))
  }

  /**
   * The polygonal regions of the Voronoi diagram.  There exists one such convex polygon for each
   * distinct vector of verts.
   */
  def voronoiCells(): Iterator[Polygon] = {
    dt.faceIncidentToVertex.keysIterator.map(voronoiCell(_))
  }

  /**
   * Provides an iterator over the Voronoi cells of the diagram and the points that defined the
   * corresponding polygonal regions.
   */
  def voronoiCellsWithPoints(): Iterator[(Polygon, Point)] = {
    dt.faceIncidentToVertex.keysIterator.map{ i:Int => (voronoiCell(i), verts(i)) }
  }

}
