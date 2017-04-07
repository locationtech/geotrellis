package geotrellis.vector.voronoi

import com.vividsolutions.jts.geom.Coordinate
import org.apache.commons.math3.linear._

import geotrellis.util.Constants.DOUBLE_EPSILON
import geotrellis.vector._
import geotrellis.vector.triangulation._

import scala.collection.mutable.Map
import scala.math.{abs,sqrt}

object VoronoiDiagram {

  def apply(pts: Array[Coordinate], extent: Extent) = new VoronoiDiagram(DelaunayTriangulation(pts), extent)

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
      if (abs(len2) < 1e-16)
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

  private def rayLineIntersection(base: V2, normal: V2, a: V2, b: V2): Option[(Double, Coordinate)] = {
    // (base, normal) herein called the "ray"
    // (a,b) herein called the "edge"

    val num = normal dot (a-base)
    val den = normal dot (a-b)
    
    if (abs(den) < 1e-16) {
      // ray and edge are parallel
      None
      /*if (abs(num) < 1e-16) {
        // edge lies on ray => base is intersection
        val x = b - a
        val y = base - a
        val r = (x dot y) / (x dot x)
        if (0 <= r && r < 1)
          Some((r, base.toCoord))
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
          Some(((pt - base).length, pt.toCoord))
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
  case class BoundedRay(base: Coordinate, dir: V2) extends CellBound
  case class Ray(base: Coordinate, dir: V2) extends CellBound
  case class ReverseRay(base: Coordinate, dir: V2) extends CellBound

  private def rayExtentIntersection(base: V2, normal: V2, extent: Extent): List[(Double, Coordinate, Int)] = {
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

  private def firstRayExtentIntersection(ray: CellBound, extent:Extent): Option[(Coordinate, Int)] = {
    def ascending(x: (Double, Coordinate, Int), y: (Double, Coordinate, Int)): Boolean = x._1 < y._1
    def descending(x: (Double, Coordinate, Int), y: (Double, Coordinate, Int)): Boolean = x._1 > y._1

    val candidates = ray match {
      case BoundedRay(base, dir) => {
        val maxlen = dir.length
        rayExtentIntersection(V2(base), dir.rot90CCW, extent)
          .filter{ case ((d, _, _)) => d < maxlen }
          .sortWith(ascending)
      }
      case Ray(base, dir) => {
        if (extent.contains(base))
          List((0.0, base, INTERIOR))
        else
          Nil
      }
      case ReverseRay(base, dir) => rayExtentIntersection(V2(base), dir.rot90CCW, extent).sortWith(descending)
    }

    candidates match {
      case Nil => None: Option[(Coordinate, Int)]
      case ((_, x, e) :: _) => Some((x, e))
    }
  }

  private def lastRayExtentIntersection(ray: CellBound, extent: Extent): Option[(Coordinate, Int)] = {
    def descending(x: (Double, Coordinate, Int), y: (Double, Coordinate, Int)): Boolean = x._1 > y._1

    val candidates = ray match {
      case BoundedRay(base, dir) => {
        val maxlen = dir.length
        if (extent.covers(dir + base)) {
          List((maxlen, dir + base, INTERIOR))
        } else {
          rayExtentIntersection(V2(base), dir.rot90CCW, extent)
            .filter{ case ((d, _, _)) => d <= maxlen }
            .sortWith(descending)
        }
      }
      case Ray(base, dir) => rayExtentIntersection(V2(base), dir.rot90CCW, extent).sortWith(descending)
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

  def faceCenter(het: HalfEdgeTable, boundEs: collection.mutable.Set[Int], verts: Int => Coordinate)(e0: Int): Option[Coordinate] = {
    if (boundEs.contains(e0)) {
      None
    } else {
      import het._
      val a = verts(getSrc(e0))
      val b = verts(getDest(e0))
      val c = verts(getDest(getNext(e0)))
      Some(RobustPredicates.circleCenter(a.x, a.y, b.x, b.y, c.x, c.y)._2)
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
  private def cellBounds(het: HalfEdgeTable, boundEs: collection.mutable.Set[Int], verts: Int => Coordinate, extent: Extent)(incidentEdge: Int): Seq[CellBound] = {
    import het._
    val getFace = faceCenter(het, boundEs, verts)(_)

    var e = incidentEdge
    val l = collection.mutable.ListBuffer.empty[CellBound]
    do {
      (getFace(e), getFace(getFlip(e))) match {
        // Two points in "triangulation"
        case (None, None) => {
          val distal = V2(verts(getSrc(e)))
          val proximal = V2(verts(getDest(e)))
          val mid = ((distal + proximal) * 0.5).toCoord
          val dir = distal - proximal
          l ++= List(Ray(mid, dir.rot90CCW), ReverseRay(mid, dir.rot90CW))
        }
        // Boundary edge (ray to infinity)
        case (Some(c), None) => l += Ray(c, (V2(verts(getDest(e))) - V2(verts(getSrc(e)))).rot90CW)
        // Boundary edge (reverse ray from infinity)
        case (None, Some(c)) => l += ReverseRay(c, (V2(verts(getDest(e))) - V2(verts(getSrc(e)))).rot90CCW)
        // Normal edge
        case (Some(c), Some(d)) if (c.distance(d) > DOUBLE_EPSILON) => l += BoundedRay(c, V2(d) - V2(c))
      }
      e = rotCCWDest(e)
    } while (e != incidentEdge)
    l
  }

  private def infill(fst: (Coordinate, Int), snd: (Coordinate, Int), extent: Extent): Seq[Coordinate] = {
    val accum = collection.mutable.ListBuffer.empty[Coordinate]
    var i = fst._2
    
    while (i != snd._2) {
      i match {
        case TOP_EDGE    => accum += new Coordinate(extent.xmin, extent.ymax)
        case LEFT_EDGE   => accum += new Coordinate(extent.xmin, extent.ymin)
        case BOTTOM_EDGE => accum += new Coordinate(extent.xmax, extent.ymin)
        case RIGHT_EDGE  => accum += new Coordinate(extent.xmax, extent.ymax)
      }
      i = (i+1)%4
    }
    accum += snd._1
    accum
  }

  private def polygonalCell(het: HalfEdgeTable, boundEs: collection.mutable.Set[Int], verts: Int => Coordinate, extent: Extent)(incidentEdge: Int): Polygon = {
    val cell = cellBounds(het, boundEs, verts, extent)(incidentEdge)
    var i = 0
    val accum = collection.mutable.ListBuffer.empty[Coordinate]

    while (i < cell.length) {
      val lrei = lastRayExtentIntersection(cell(i), extent)
      i = i + 1
      if (lrei != None) {
        val (cellVert, vertLoc) = lrei.get
        accum += cellVert
        if (vertLoc != INTERIOR) {
          var frei = firstRayExtentIntersection(cell(i % cell.length), extent)
          while(frei == None) {
            i = i + 1
            frei = firstRayExtentIntersection(cell(i % cell.length), extent)
          }
          accum ++= infill(lrei.get, frei.get, extent)
        }
      }
    }

    accum += accum.head
    Polygon(accum.map(Point.jtsCoord2Point(_)))
  }

}

/**
 * A class to compute the Voronoi diagram of a set of points.  See
 * <geotrellis_home>/docs/vector/voronoi.md for more information.
 */
class VoronoiDiagram(val dt: DelaunayTriangulation, val extent: Extent) {
  import VoronoiDiagram._

  val pointSet = dt.pointSet
  private val boundEs = collection.mutable.Set.empty[Int]
  dt.halfEdgeTable.foreachInLoop(dt.boundary){ e => boundEs += e }

  // /*
  //  * A method to generate the Voronoi cell corresponding to the point in verts(incidentEdge.vert).
  //  * If the incident edge is not an interior edge (incidentEdge.face == None) the results are
  //  * undefined.
  //  */
  // def voronoiCell(incidentEdge: HalfEdge[Int, Coordinate]): Polygon = {
  //   val cell = cellBounds(incidentEdge)
  //   var i = 0
  //   var accum: List[Coordinate] = Nil

  //   while (i < cell.length) {
  //     val lrei = lastRayExtentIntersection(cell(i))
  //     i = i + 1
  //     if (lrei != None) {
  //       val (cellVert, vertLoc) = lrei.get
  //       accum = accum :+ cellVert
  //       if (vertLoc != INTERIOR) {
  //         var frei = firstRayExtentIntersection(cell(i % cell.length))
  //         while(frei == None) {
  //           i = i + 1
  //           frei = firstRayExtentIntersection(cell(i % cell.length))
  //         }
  //         accum = accum ++ infill(lrei.get, frei.get)
  //       }
  //     }
  //   }

  //   Polygon(Line(accum).closed)
  // }

  /**
   * A method to generate the Voronoi cell corresponding to the point in verts(i).  Note that if
   * verts(i) is not distinct, this function may raise an exception.
   */
  def voronoiCell(i: Int): Polygon = {
    polygonalCell(dt.halfEdgeTable, boundEs, pointSet.getCoordinate(_), extent)(dt.halfEdgeTable.edgeIncidentTo(i))
  }

  /**
   * The polygonal regions of the Voronoi diagram.  There exists one such convex polygon for each
   * distinct vector of verts.
   */
  def voronoiCells(): Seq[Polygon] = {
    dt.halfEdgeTable.allVertices.toSeq.map(voronoiCell(_))
  }

  /**
   * Provides an iterator over the Voronoi cells of the diagram and the points that defined the
   * corresponding polygonal regions.
   */
  def voronoiCellsWithPoints(): Seq[(Polygon, Coordinate)] = {
    dt.halfEdgeTable.allVertices.toSeq.map{ i:Int => (voronoiCell(i), dt.pointSet.getCoordinate(i)) }
  }

}
