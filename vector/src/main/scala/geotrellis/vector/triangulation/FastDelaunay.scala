package geotrellis.vector.triangulation

import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}
import geotrellis.vector._
import geotrellis.vector.io._
import scala.math.{abs, pow}

import scala.collection.mutable
import scala.collection.mutable.Map
import scala.annotation.tailrec

/**
 * A class to compute the Delaunay triangulation of a set of points.  Details can be found in
 * <geotrellis_home>/docs/guide/vectors.md
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

  val vIx = (0 until verts.length).toList.sortWith{
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
        }

  private def triangulate(lo: Int, hi: Int): (HalfEdge[Int,LightPoint], Boolean) = {
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
        (e, true)
      }
      case 3 if (isCCW(vIx(lo), vIx(lo+1), vIx(lo+2))) => {
        val t = HalfEdge[Int,LightPoint](List(vIx(lo), vIx(lo+1), vIx(lo+2)),
                                    circleCenter(vIx(lo), vIx(lo+1), vIx(lo+2))).prev
        insertTriangle((vIx(lo), vIx(lo+1), vIx(lo+2)), t)
        (t, false)
      }
      case 3 if (isCCW(vIx(lo), vIx(lo+2), vIx(lo+1))) => {
        val t = HalfEdge[Int,LightPoint](List(vIx(lo), vIx(lo+2), vIx(lo+1)),
                                    circleCenter(vIx(lo), vIx(lo+2), vIx(lo+1)))
        insertTriangle((vIx(lo), vIx(lo+2), vIx(lo+1)), t)
        (t, false)
      }
      case 3 => {
        val a = HalfEdge[Int,LightPoint](vIx(lo), vIx(lo+1))
        val b = HalfEdge[Int,LightPoint](vIx(lo+1), vIx(lo+2))
        a.next = b
        b.flip.next = a.flip
        (b.flip, true)
      }
      case _ => {
        val med = (hi + lo) / 2
        var (left, isLeftLinear) = triangulate(lo,med)
        var (right, isRightLinear) = triangulate(med+1,hi)

        if (isLeftLinear) {
          while (verts(left.src).x - verts(left.vert).x < -EPSILON ||
                 (abs(verts(left.src).x - verts(left.vert).x) < EPSILON && verts(left.src).y - verts(left.vert).y < -EPSILON))
            left = left.next
        }
        if (isRightLinear) {
          while (verts(right.src).x - verts(right.flip.next.vert).x > EPSILON ||
                 (abs(verts(right.src).x - verts(right.flip.next.vert).x) < EPSILON
                  && verts(right.src).y - verts(right.flip.next.vert).y > EPSILON))
            right = right.flip.next.flip
        }

        // compute the lower common tangent of left and right
        var continue = true
        var base: HalfEdge[Int, LightPoint] = null
        while (continue) {
          base = HalfEdge[Int,LightPoint](right.src, left.src)
          if (isLeftOf(base, left.vert)) {
            // left points to a vertex that is to the left of
            // base, so move base to left.next
            left = left.next
          } else if (isLeftOf(base, right.vert)) {
            // right points to a point that is left of base,
            // so keep walking right
            right = right.next
          } else if (isLeftOf(base, left.prev.src)) {
            // Left's previous source is left of base,
            // so this base would break convexity. Move
            // back to previous left.
            left = left.prev
          } else if (isLeftOf(base, right.prev.src)) {
            // Right's previous source is left ofbase,
            // so this base would break convexity. Move
            // back to previous right.
            right = right.prev
          } else if(isCollinear(base, right.vert) &&
                    !isRightLinear) {
            right = right.next
          } else {
            continue = false
          }
        }

        base.next = left
        base.flip.next = right
        left.prev.next = base.flip
        right.prev.next = base

        // If linear joins to linear, check that the current state isn't already done (linear result)
        if (isLeftLinear && isRightLinear) {
          val b0 = verts(base.src)
          val b1 = verts(base.vert)
          val l = verts(base.next.vert)
          val r = verts(base.flip.next.vert)
          if (isCollinear(b0, b1, l)  && isCollinear(b0, b1, r)) {
            return (base.flip, true)
          }
        }

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

        (base.flip.next, false)
      }
    }
  }

  /**
   * Provides a handle on the bounding loop of a Delaunay triangulation.  All of the half-edges
   * reachable by applying next to boundary have no bounding face information.  That is
   * boundary(.next)*.face == None.
   */
  val (boundary, isLinear) = triangulate(0, vIx.length-1)

}
