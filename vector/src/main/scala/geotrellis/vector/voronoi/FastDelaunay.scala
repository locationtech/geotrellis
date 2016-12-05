package geotrellis.vector.voronoi

import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}
import geotrellis.vector._
import scala.math.pow
import org.apache.commons.math3.linear._

import scala.collection.mutable.Map
import scala.annotation.tailrec

case class LightPoint(x: Double, y: Double) {
  def distance(other: LightPoint): Double = {
    val dx = other.x - x
    val dy = other.y - y
    math.sqrt(dx*dx + dy*dy)
  }

  def toPoint: Point =
    Point(x, y)
}

object Predicates {
  def det3 (a11: Double, a12: Double, a13: Double,
            a21: Double, a22: Double, a23: Double,
            a31: Double, a32: Double, a33: Double): Double = {
    val m = MatrixUtils.createRealMatrix(Array(Array(a11, a12, a13),
                                               Array(a21, a22, a23),
                                               Array(a31, a32, a33)))
    (new LUDecomposition(m)).getDeterminant
  }


  def isCCW(a: LightPoint, b: LightPoint, c: LightPoint): Boolean =
    ShewchuksDeterminant.orient2d(a.x, a.y, b.x, b.y, c.x, c.y) > 0

  def isRightOf[T](e: HalfEdge[Int,T], p: LightPoint)(implicit trans: Int => LightPoint) =
    isCCW(p, trans(e.vert), trans(e.src))

  def isRightOf[T](e: HalfEdge[Int,T], p: Int)(implicit trans: Int => LightPoint) =
    isCCW(trans(p), trans(e.vert), trans(e.src))

  def isLeftOf[T](e: HalfEdge[Int,T], p: LightPoint)(implicit trans: Int => LightPoint) =
    isCCW(p, trans(e.src), trans(e.vert))

  def isLeftOf[T](e: HalfEdge[Int,T], p: Int)(implicit trans: Int => LightPoint) =
    isCCW(trans(p), trans(e.src), trans(e.vert))

  def inCircle(abc: (LightPoint, LightPoint, LightPoint), d: LightPoint): Boolean = {
    val (a,b,c) = abc
    ShewchuksDeterminant.incircle(a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y) > 0
  }


  def circleCenter(a: LightPoint, b: LightPoint, c: LightPoint): LightPoint = {
    val d = 2.0 * det3(a.x, a.y, 1.0,
                       b.x, b.y, 1.0,
                       c.x, c.y, 1.0)
    val h = det3(a.x * a.x + a.y * a.y, a.y, 1.0,
                 b.x * b.x + b.y * b.y, b.y, 1.0,
                 c.x * c.x + c.y * c.y, c.y, 1.0) / d
    val k = det3(a.x, a.x * a.x + a.y * a.y, 1.0,
                 b.x, b.x * b.x + b.y * b.y, 1.0,
                 c.x, c.x * c.x + c.y * c.y, 1.0) / d
    LightPoint(h,k)
  }

  def circleCenter(ai: Int, bi: Int, ci: Int)(implicit trans: Int => LightPoint): LightPoint = {
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
    LightPoint(h,k)
  }

  def isDelaunayEdge[T](e: HalfEdge[Int,T])(implicit trans: Int => LightPoint): Boolean = {
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
 * A class to compute the Delaunay triangulation of a set of points.  Details can be found in
 * <geotrellis_home>/docs/vector/voronoi.md
 */
case class FastDelaunay(verts: Array[LightPoint]) {
  private def distinctPoints(lst: List[Int]): List[Int] = {
    @tailrec def dpInternal(l: List[Int], acc: List[Int]): List[Int] = {
      l match {
        case Nil => acc
        case i :: Nil => i :: acc
        case i :: rest => {
          val chunk = rest.takeWhile{j => verts(j).x <= verts(i).x + 1e-8}
          if (chunk.exists{j => verts(j).distance(verts(i)) < 1e-10})
            dpInternal(rest, acc)
          else
            dpInternal(rest, i :: acc)
        }
      }
    }
    dpInternal(lst, Nil).reverse
  }

  type TriangleMap = Map[(Int, Int, Int), HalfEdge[Int, LightPoint]]

  private val _triangles: TriangleMap =
    Map.empty[(Int, Int, Int), HalfEdge[Int, LightPoint]]

  /**
   * A catalog of the triangles produced by the Delaunay triangulation as a Map from (Int,Int,Int)
   * to HalfEdge[Int,LightPoint].  All integers are indices of points in verts, while the LightPoints describe
   * the center of the circumscribing circle for the triangle.  The Int triples give the triangle
   * vertices in counter-clockwise order, starting from the lowest-numbered vertex.
   */
  lazy val triangles = _triangles.toMap

  val _faceIncidentToVertex = Map.empty[Int, HalfEdge[Int, LightPoint]]

  /**
   * A catalog of edges incident on the vertices of a Delaunay triangulation.  These half edges may
   * be used to navigate the faces that have a given point as a vertex.  For example, all edges
   * incident on vertex i may be visited by repeated application of the rotCCWDest() method of
   * HalfEdge on faceIncidentToVertex(i).
   */
  lazy val faceIncidentToVertex = _faceIncidentToVertex.toMap

  private def regularizeTriangleIndex (index: (Int, Int, Int)): (Int, Int, Int) = {
    index match {
      case (a, b, c) if (a < b && a < c) => (a, b, c)
      case (a, b, c) if (b < a && b < c) => (b, c, a)
      case (a, b, c) => (c, a, b)
    }
  }

  private def insertTriangle(vs: (Int, Int, Int), e: HalfEdge[Int, LightPoint]): Map[(Int, Int, Int), HalfEdge[Int, LightPoint]] = {
    val idx = regularizeTriangleIndex(vs)
    _faceIncidentToVertex += (e.vert -> e, e.next.vert -> e.next, e.next.next.vert -> e.next.next)
    _triangles += (idx -> e)
  }

  private def insertTriangle(e: HalfEdge[Int,LightPoint]): Map[(Int,Int,Int),HalfEdge[Int,LightPoint]] = {
    insertTriangle((e.vert, e.next.vert, e.next.next.vert), e)
  }

  private def deleteTriangle(vs: (Int,Int,Int)): Map[(Int,Int,Int),HalfEdge[Int,LightPoint]] = {
    val idx = regularizeTriangleIndex(vs)
    _triangles -= idx
  }

  private def deleteTriangle(e: HalfEdge[Int,LightPoint]): Map[(Int,Int,Int),HalfEdge[Int,LightPoint]] = {
    deleteTriangle((e.vert, e.next.vert, e.next.next.vert))
  }

  private def lookupTriangle(vs: (Int,Int,Int)) = {
    val idx = regularizeTriangleIndex(vs)
    _triangles(idx)
  }

  val vIx = distinctPoints((0 until verts.length).toList.sortWith{
    (i1,i2) => {
      val p1 = verts(i1)
      val p2 = verts(i2)
      if (p1.x < p2.x) {
        true
      } else {
        if (p1.x > p2.x) {
          false
        } else {
          p1.y < p2.y
        }
      }
    }
  }).toArray

  private def triangulate(lo: Int, hi: Int): HalfEdge[Int,LightPoint] = {
    // Implementation follows Guibas and Stolfi, ACM Transations on Graphics, vol. 4(2), 1985, pp. 74--123
    val n = hi - lo + 1
    implicit val trans = { i: Int => verts(i) }
    import Predicates._

    n match {
      case 1 => {
        throw new IllegalArgumentException("Cannot triangulate a point set of size less than 2")
      }
      case 2 => {
        val e = HalfEdge[Int,LightPoint](vIx(lo), vIx(hi))
        _faceIncidentToVertex += (e.vert -> e, e.src -> e.flip)
        e
      }
      case 3 if (isCCW(vIx(lo), vIx(lo+1), vIx(lo+2))) => {
        val t = HalfEdge[Int,LightPoint](List(vIx(lo), vIx(lo+1), vIx(lo+2)),
                                    circleCenter(vIx(lo), vIx(lo+1), vIx(lo+2))).prev
        insertTriangle((vIx(lo), vIx(lo+1), vIx(lo+2)), t)
        t
      }
      case 3 if (isCCW(vIx(lo), vIx(lo+2), vIx(lo+1))) => {
        val t = HalfEdge[Int,LightPoint](List(vIx(lo), vIx(lo+2), vIx(lo+1)),
                                    circleCenter(vIx(lo), vIx(lo+2), vIx(lo+1)))
        insertTriangle((vIx(lo), vIx(lo+2), vIx(lo+1)), t)
        t
      }
      case 3 => {
        val a = HalfEdge[Int,LightPoint](vIx(lo), vIx(lo+1))
        val b = HalfEdge[Int,LightPoint](vIx(lo+1), vIx(lo+2))
        a.next = b
        b.flip.next = a.flip
        b.flip
      }
      case _ => {
        val med = (hi + lo) / 2
        var left = triangulate(lo,med)
        var right = triangulate(med+1,hi)

        // compute the lower common tangent of left and right
        //  NOTE: failures in the Delaunay triangulator are likely due to a failure in this step
        var continue = true
        var base: HalfEdge[Int, LightPoint] = null
        while (continue) {
          base = HalfEdge[Int,LightPoint](right.src, left.src)
          if (isLeftOf(base, left.vert)) {
            left = left.next
          } else if (isLeftOf(base, right.vert)) {
            right = right.next
          } else if (isLeftOf(base, left.prev.src)) {
            left = left.prev
          } else if (isLeftOf(base, right.prev.src)) {
            right = right.prev
          } else {
            continue = false
          }
        }

        base.next = left
        base.flip.next = right
        left.prev.next = base.flip
        right.prev.next = base

        continue = true
        while (continue) {
          var lcand = base.flip.rotCCWSrc
          var rcand = base.rotCWSrc

          // Find left side candidate edge for extending the fill triangulation
          if (isCCW(lcand.vert, base.vert, base.src)) {
            while (inCircle((base.vert, base.src, lcand.vert), lcand.rotCCWSrc.vert)) {
              val e = lcand.rotCCWSrc
              deleteTriangle(lcand)
              lcand.rotCCWDest.next = lcand.next
              lcand.prev.next = lcand.flip.next
              lcand = e
            }
          }

          // Find right side candidate edge for extending the fill triangulation
          if (isCCW(rcand.vert, base.vert, base.src)) {
            while (inCircle((base.vert, base.src, rcand.vert), rcand.rotCWSrc.vert)) {
              val e = rcand.rotCWSrc
              deleteTriangle(rcand.flip)
              base.flip.next = rcand.rotCWSrc
              rcand.rotCCWDest.next = rcand.next
              rcand = e
            }
          }


          if (!isCCW(lcand.vert, base.vert, base.src) && !isCCW(rcand.vert, base.vert, base.src)) {
            // no further Delaunay triangles to add
            continue = false
          } else {
            if (!isCCW(lcand.vert, base.vert, base.src) ||
                (isCCW(rcand.vert, base.vert, base.src) &&
                 inCircle((lcand.vert, lcand.src, rcand.src), rcand.vert)))
            {
              // form new triangle from rcand and base
              val e = HalfEdge[Int,LightPoint](rcand.vert, base.vert)
              e.flip.next = rcand.next
              e.next = base.flip
              rcand.next = e
              lcand.flip.next = e.flip
              base = e
            } else {
              // form new triangle from lcand and base
              val e = HalfEdge[Int,LightPoint](base.src, lcand.vert)
              lcand.rotCCWDest.next = e.flip
              e.next = lcand.flip
              e.flip.next = rcand
              base.flip.next = e
              base = e
            }
            // Tag edges with the center of the new triangle's circumscribing circle
            val c = circleCenter(base.vert, base.next.vert, base.next.next.vert)
            base.face = Some(c)
            base.next.face = Some(c)
            base.next.next.face = Some(c)
            insertTriangle(base)
          }
        }

        base.flip.next
      }
    }
  }

  /**
   * Provides a handle on the bounding loop of a Delaunay triangulation.  All of the half-edges
   * reachable by applying next to boundary have no bounding face information.  That is
   * boundary(.next)*.face == None.
   */
  val boundary = triangulate(0, vIx.length-1)
}
