package geotrellis.vector.voronoi

import geotrellis.util.Constants.{FLOAT_EPSILON => EPSILON}
import geotrellis.vector.{Point, Polygon}

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, MultiPoint, Polygon => JTSPolygon}
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder
import com.vividsolutions.jts.triangulate.quadedge.{QuadEdge}
import org.apache.commons.math3.linear._

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.mutable.{Map, Set}
import scala.math.pow
import spire.syntax.cfor._

object Predicates {
  def det3 (a11: Double, a12: Double, a13: Double,
            a21: Double, a22: Double, a23: Double,
            a31: Double, a32: Double, a33: Double): Double = {
    val m = MatrixUtils.createRealMatrix(Array(Array(a11, a12, a13),
                                               Array(a21, a22, a23),
                                               Array(a31, a32, a33)))
    (new LUDecomposition(m)).getDeterminant
  }
    

  def isCCW(a: Point, b: Point, c: Point): Boolean = {
    // det [ a.x-c.x  a.y-c.y ]
    //     [ b.x-c.x  b.y-c.y ] > 0
    (a.x - c.x) * (b.y - c.y) - (a.y - c.y) * (b.x - c.x) > EPSILON
  }

  def isCCW(ai: Int, bi: Int, ci: Int)(implicit trans: Int => Point): Boolean = {
    val a = trans(ai)
    val b = trans(bi)
    val c = trans(ci)
    (a.x - c.x) * (b.y - c.y) - (a.y - c.y) * (b.x - c.x) > EPSILON
  }

  def isRightOf[T](e: HalfEdge[Int,T], p: Point)(implicit trans: Int => Point) = 
    isCCW(p, trans(e.vert), trans(e.src))

  def isRightOf[T](e: HalfEdge[Int,T], p: Int)(implicit trans: Int => Point) = 
    isCCW(trans(p), trans(e.vert), trans(e.src))

  def isLeftOf[T](e: HalfEdge[Int,T], p: Point)(implicit trans: Int => Point) = 
    isCCW(p, trans(e.src), trans(e.vert))

  def isLeftOf[T](e: HalfEdge[Int,T], p: Int)(implicit trans: Int => Point) = 
    isCCW(trans(p), trans(e.src), trans(e.vert))

  def inCircle(abc: (Point, Point, Point), d: Point): Boolean = {
    val (a,b,c) = abc
    det3(a.x - d.x, a.y - d.y, pow(a.x - d.x, 2) + pow(a.y - d.y, 2),
         b.x - d.x, b.y - d.y, pow(b.x - d.x, 2) + pow(b.y - d.y, 2),
         c.x - d.x, c.y - d.y, pow(c.x - d.x, 2) + pow(c.y - d.y, 2)) > EPSILON
  }

  def inCircle(abc: (Int, Int, Int), di: Int)(implicit trans: Int => Point): Boolean = {
    val (ai,bi,ci) = abc
    val a = trans(ai)
    val b = trans(bi)
    val c = trans(ci)
    val d = trans(di)
    det3(a.x - d.x, a.y - d.y, pow(a.x - d.x, 2) + pow(a.y - d.y, 2),
         b.x - d.x, b.y - d.y, pow(b.x - d.x, 2) + pow(b.y - d.y, 2),
         c.x - d.x, c.y - d.y, pow(c.x - d.x, 2) + pow(c.y - d.y, 2)) > EPSILON
  }

  def circleCenter(a: Point, b: Point, c: Point): Point = {
    val d = 2.0 * det3(a.x, a.y, 1.0, 
                       b.x, b.y, 1.0, 
                       c.x, c.y, 1.0)
    val h = det3(a.x * a.x + a.y * a.y, a.y, 1.0, 
                 b.x * b.x + b.y * b.y, b.y, 1.0, 
                 c.x * c.x + c.y * c.y, c.y, 1.0) / d
    val k = det3(a.x, a.x * a.x + a.y * a.y, 1.0, 
                 b.x, b.x * b.x + b.y * b.y, 1.0, 
                 c.x, c.x * c.x + c.y * c.y, 1.0) / d
    Point(h,k)
  }

  def circleCenter(ai: Int, bi: Int, ci: Int)(implicit trans: Int => Point): Point = {
    val a = trans(ai)
    val b = trans(bi)
    val c = trans(ci)
    val d = 2.0 * det3(a.x, a.y, 1.0, 
                       b.x, b.y, 1.0, 
                       c.x, c.y, 1.0)
    val h = det3(a.x * a.x + a.y * a.y, a.y, 1.0, 
                 b.x * b.x + b.y * b.y, b.y, 1.0, 
                 c.x * c.x + c.y * c.y, c.y, 1.0) / d
    val k = det3(a.x, a.x * a.x + a.y * a.y, 1.0, 
                 b.x, b.x * b.x + b.y * b.y, 1.0, 
                 c.x, c.x * c.x + c.y * c.y, 1.0) / d
    Point(h,k)
  }

  def isDelaunayEdge[T](e: HalfEdge[Int,T])(implicit trans: Int => Point): Boolean = {
    // Predicated on the fact that if an edge is Delaunay, then for a
    // point, A, to the left of edge (X,Y), and a point, B, to the
    // right of (X,Y), A may not be in the circle defined by points X,
    // Y, and B.
    val a = trans(e.next.vert)
    val b = trans(e.flip.next.vert)
    val x = trans(e.flip.vert)
    val y = trans(e.vert)
    !inCircle((a, x, y), b)
  }
}

/**
 * A class for triangulating a set of points to satisfy the delaunay property.
 * Each resulting triangle's circumscribing circle will contain no other points
 * of the input set.
 */
case class Delaunay(verts: Array[Point]) {

  private[voronoi] val gf = new GeometryFactory
  private val sites = new MultiPoint(verts.map(_.jtsGeom), gf)
  private val builder = new DelaunayTriangulationBuilder
  builder.setSites(sites)
  private[voronoi] val subd = builder.getSubdivision

  val triangles: Seq[Polygon] = {
    val tris = subd.getTriangles(gf)
    val len = tris.getNumGeometries
    val arr = Array.ofDim[Polygon](len)
    cfor(0)(_ < len, _ + 1) { i => arr(i) = Polygon(tris.getGeometryN(i).asInstanceOf[JTSPolygon]) }
    arr
}

}
