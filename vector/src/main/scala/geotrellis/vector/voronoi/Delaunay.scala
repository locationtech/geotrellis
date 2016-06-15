package geotrellis.vector.voronoi

import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}
import geotrellis.vector._
import scala.math.pow
import org.apache.commons.math3.linear._
import scala.collection.mutable.Map
//import org.apache.log4j.Logger

object Predicates {
  def det3 (a11: Double, a12: Double, a13: Double,
            a21: Double, a22: Double, a23: Double,
            a31: Double, a32: Double, a33: Double): Double = {
    val m = MatrixUtils.createRealMatrix(Array(Array(a11,a12,a13),Array(a21,a22,a23),Array(a31,a32,a33)))
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
    det3(a.x-d.x, a.y-d.y, pow(a.x-d.x,2) + pow(a.y-d.y,2),
         b.x-d.x, b.y-d.y, pow(b.x-d.x,2) + pow(b.y-d.y,2),
         c.x-d.x, c.y-d.y, pow(c.x-d.x,2) + pow(c.y-d.y,2)) > EPSILON
  }

  def inCircle(abc: (Int, Int, Int), di: Int)(implicit trans: Int => Point): Boolean = {
    val (ai,bi,ci) = abc
    val a = trans(ai)
    val b = trans(bi)
    val c = trans(ci)
    val d = trans(di)
    det3(a.x-d.x, a.y-d.y, pow(a.x-d.x,2) + pow(a.y-d.y,2),
         b.x-d.x, b.y-d.y, pow(b.x-d.x,2) + pow(b.y-d.y,2),
         c.x-d.x, c.y-d.y, pow(c.x-d.x,2) + pow(c.y-d.y,2)) > EPSILON
  }

  def circleCenter(a: Point, b: Point, c: Point): Point = {
    val d = 2.0 * det3(a.x,a.y,1.0, b.x,b.y,1.0, c.x,c.y,1.0)
    val h = det3(a.x*a.x+a.y*a.y,a.y,1.0, b.x*b.x+b.y*b.y,b.y,1.0, c.x*c.x+c.y*c.y,c.y,1.0) / d
    val k = det3(a.x,a.x*a.x+a.y*a.y,1.0, b.x,b.x*b.x+b.y*b.y,1.0, c.x,c.x*c.x+c.y*c.y,1.0) / d
    Point(h,k)
  }

  def circleCenter(ai: Int, bi: Int, ci: Int)(implicit trans: Int => Point): Point = {
    val a = trans(ai)
    val b = trans(bi)
    val c = trans(ci)
    val d = 2.0 * det3(a.x,a.y,1.0, b.x,b.y,1.0, c.x,c.y,1.0)
    val h = det3(a.x*a.x+a.y*a.y,a.y,1.0, b.x*b.x+b.y*b.y,b.y,1.0, c.x*c.x+c.y*c.y,c.y,1.0) / d
    val k = det3(a.x,a.x*a.x+a.y*a.y,1.0, b.x,b.x*b.x+b.y*b.y,1.0, c.x,c.x*c.x+c.y*c.y,1.0) / d
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
    !inCircle((a,x,y),b)
  }
}

/**
 * A class to compute the Delaunay triangulation of a set of points.  Details can be found in
 * <geotrellis_home>/docs/vector/voronoi.md
 */
case class Delaunay(verts: Array[Point]) {
  /**
   * A catalog of the triangles produced by the Delaunay triangulation as a Map from (Int,Int,Int)
   * to HalfEdge[Int,Point].  All integers are indices of points in verts, while the Points describe
   * the center of the circumscribing circle for the triangle.  The Int triples give the triangle
   * vertices in counter-clockwise order, starting from the lowest-numbered vertex.
   */
  val triangles = Map.empty[(Int,Int,Int),HalfEdge[Int,Point]]

  /**
   * A catalog of edges incident on the vertices of a Delaunay triangulation.  These half edges may
   * be used to navigate the faces that have a given point as a vertex.  For example, all edges
   * incident on vertex i may be visited by repeated application of the rotCCWDest() method of
   * HalfEdge on faceIncidentToVertex(i).
   */
  val faceIncidentToVertex = Map.empty[Int,HalfEdge[Int,Point]]

  private def insertTriangle(vs: (Int,Int,Int), e: HalfEdge[Int,Point]): Map[(Int,Int,Int),HalfEdge[Int,Point]] = {
    val idx = vs match {
      case (a,b,c) if (a<b && a<c) => (a,b,c)
      case (a,b,c) if (b<a && b<c) => (b,c,a)
      case (a,b,c) => (c,a,b)
    }
    //println(s"Inserting $e as $vs [using $idx as key]")
    faceIncidentToVertex += (e.vert -> e, e.next.vert -> e.next, e.next.next.vert -> e.next.next)
    //println(s"Adding to faceIncidentToVertex: ${e.vert -> e}, ${e.next.vert -> e.next}, ${e.next.next.vert -> e.next.next}")
    triangles += (idx -> e)
  }

  private def insertTriangle(e: HalfEdge[Int,Point]): Map[(Int,Int,Int),HalfEdge[Int,Point]] = {
    insertTriangle((e.vert,e.next.vert,e.next.next.vert), e)
  }

  private def deleteTriangle(vs: (Int,Int,Int)): Map[(Int,Int,Int),HalfEdge[Int,Point]] = {
    val idx = vs match {
      case (a,b,c) if (a<b && a<c) => (a,b,c)
      case (a,b,c) if (b<a && b<c) => (b,c,a)
      case (a,b,c) => (c,a,b)
    }
    //println(s"Removing $vs [using $idx as key]")
    triangles -= idx
  }

  private def deleteTriangle(e: HalfEdge[Int,Point]): Map[(Int,Int,Int),HalfEdge[Int,Point]] = {
    deleteTriangle((e.vert,e.next.vert,e.next.next.vert))
  }

  private def lookupTriangle(vs: (Int,Int,Int)) = {
    val idx = vs match {
      case (a,b,c) if (a<b && a<c) => (a,b,c)
      case (a,b,c) if (b<a && b<c) => (b,c,a)
      case (a,b,c) => (c,a,b)
    }
    triangles(idx)
  }

  private def distinctBy[T](eq: (T,T) => Boolean)(l: List[T]): List[T] = {
    l match {
      case Nil => Nil
      case x::Nil => x::Nil
      case x::y::xs if eq(x,y) => distinctBy(eq)(x::xs)
      case x::y::xs => x :: distinctBy(eq)(y::xs)
    }
  }

  private val vIx = distinctBy{ (i: Int, j: Int) => verts(i).distance(verts(j)) < 1e-10 }(
    (0 until verts.length).toList.sortWith{
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

  private def triangulate(lo: Int, hi: Int): HalfEdge[Int,Point] = {
    // Implementation follows Guibas and Stolfi, ACM Transations on Graphics, vol. 4(2), 1985, pp. 74--123
    val n = hi - lo + 1
    implicit val trans = { i: Int => verts(i) }
    import Predicates._
    
    n match {
      case 2 => {
        //log.debug(s"Creating single edge: (${vIx(lo)},${vIx(hi)})")
        val e = HalfEdge[Int,Point](vIx(lo), vIx(hi))
        faceIncidentToVertex += (e.vert -> e, e.src -> e.flip)
        e
      }
      case 3 if (isCCW(vIx(lo), vIx(lo+1), vIx(lo+2))) => {
        //log.debug(s"Creating triangle: (${vIx(lo)},${vIx(lo+1)}, ${vIx(lo+2)})")
        val t = HalfEdge[Int,Point](List(vIx(lo), vIx(lo+1), vIx(lo+2)), 
                                    circleCenter(vIx(lo), vIx(lo+1), vIx(lo+2))).prev
        insertTriangle((vIx(lo), vIx(lo+1), vIx(lo+2)), t)
        t
      }
      case 3 if (isCCW(vIx(lo), vIx(lo+2), vIx(lo+1))) => {
        //log.debug(s"Creating triangle: (${vIx(lo)},${vIx(lo+2)}, ${vIx(lo+1)})")
        val t = HalfEdge[Int,Point](List(vIx(lo), vIx(lo+2), vIx(lo+1)), 
                                    circleCenter(vIx(lo), vIx(lo+2), vIx(lo+1)))
        insertTriangle((vIx(lo), vIx(lo+2), vIx(lo+1)), t)
        t
      }
      case 3 => {
        val a = HalfEdge[Int,Point](vIx(lo), vIx(lo+1))
        val b = HalfEdge[Int,Point](vIx(lo+1), vIx(lo+2))
        a.next = b
        b.flip.next = a.flip
        //log.debug(s"Creating edge pair: $a $b")
        b.flip
      }
      case _ => {
        val med = (hi + lo) / 2
        var left = triangulate(lo,med)
        var right = triangulate(med+1,hi)

        //log.debug(s"initial left:  (${left.src},${left.vert})")
        //log.debug(s"initial right: (${right.src},${right.vert})")

        // compute the lower common tangent of left and right
        var continue = true
        while (continue) {
          if (isLeftOf(left, right.src)) {
            left = left.next
            //log.debug(s"Advancing left to (${left.src},${left.vert})")
          } else {
            if (isRightOf(right, left.src)) {
              right = right.next
              //log.debug(s"Advancing right to (${right.src},${right.vert})")
            } else {
              continue = false
            }
          }
        }

        var base = HalfEdge[Int,Point](right.src, left.src)
        base.next = left
        base.flip.next = right
        left.prev.next = base.flip
        right.prev.next = base
        //log.debug(s"Base initialized to $base")

        continue = true
        while (continue) {
          var lcand = base.flip.rotCCWSrc
          var rcand = base.rotCWSrc

          //log.debug("Searching for left candidate ... ")
          if (isCCW(lcand.vert, base.vert, base.src)) {
            while (inCircle((base.vert, base.src, lcand.vert), lcand.rotCCWSrc.vert)) {
              val e = lcand.rotCCWSrc
              //log.debug(s"Deleting edge $lcand")
              deleteTriangle(lcand)
              lcand.rotCCWDest.next = lcand.next
              lcand.prev.next = lcand.flip.next
              lcand = e
              //HalfEdge.showBoundingLoop(base.flip)
            }
          }
          //log.debug(s"Found: $lcand")

          //log.debug("Searching for right candidate ... ")
          if (isCCW(rcand.vert, base.vert, base.src)) {
            while (inCircle((base.vert, base.src, rcand.vert), rcand.rotCWSrc.vert)) {
              val e = rcand.rotCWSrc
              //log.debug(s"Deleting edge $rcand")
              deleteTriangle(rcand.flip)
              base.flip.next = rcand.rotCWSrc
              rcand.rotCCWDest.next = rcand.next
              rcand = e
              //HalfEdge.showBoundingLoop(base.flip)
            }
          }
          //log.debug(s"Found: $rcand")

          if (!isCCW(lcand.vert, base.vert, base.src) && !isCCW(rcand.vert, base.vert, base.src)) {
            continue = false
          } else {
            if (!isCCW(lcand.vert, base.vert, base.src) ||
                (isCCW(rcand.vert, base.vert, base.src) && 
                 inCircle((lcand.vert, lcand.src, rcand.src), rcand.vert))) 
            {
              // form new triangle from rcand and base
              val e = HalfEdge[Int,Point](rcand.vert, base.vert)
              e.flip.next = rcand.next
              e.next = base.flip
              rcand.next = e
              lcand.flip.next = e.flip
              base = e
            } else {
              // form new triangle from lcand and base
              val e = HalfEdge[Int,Point](base.src, lcand.vert)
              lcand.rotCCWDest.next = e.flip
              e.next = lcand.flip
              e.flip.next = rcand
              base.flip.next = e
              base = e
            }
            val c = circleCenter(base.vert, base.next.vert, base.next.next.vert)
            base.face = Some(c)
            base.next.face = Some(c)
            base.next.next.face = Some(c)
            insertTriangle(base)
            //log.debug(s"Created face (${base.vert}, ${base.next.vert}, ${base.next.next.vert})")
            //log.debug(s"Base advanced to $base")
          }
          //HalfEdge.showBoundingLoop(base.flip)
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

// val dt = new Delaunay(List(Point(0,0),Point(3,1),Point(7,5),Point(4,7),Point(8,3),Point(1,3)).toArray)
// val t = dt.triangulate(0,5)
// val m = scala.collection.Map(0 -> Point(0,0), 1 -> Point(3,1), 2 -> Point(7,5), 3 -> Point(4,7), 4 -> Point(8,3), 5 -> Point(1,3))
