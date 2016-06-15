package geotrellis.vector.voronoi

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
    // m.setEntry(0, 0, a11)
    // m.setEntry(0, 1, a12)
    // m.setEntry(0, 2, a13)
    // m.setEntry(1, 0, a21)
    // m.setEntry(1, 1, a22)
    // m.setEntry(1, 2, a23)
    // m.setEntry(2, 0, a31)
    // m.setEntry(2, 1, a32)
    // m.setEntry(2, 2, a33)
    (new LUDecomposition(m)).getDeterminant
  }
    

  def isCCW(a: Point, b: Point, c: Point): Boolean = {
    // det [ a.x-c.x  a.y-c.y ]
    //     [ b.x-c.x  b.y-c.y ] > 0
    (a.x - c.x) * (b.y - c.y) - (a.y - c.y) * (b.x - c.x) > 0
  }

  def isCCW(ai: Int, bi: Int, ci: Int)(implicit trans: Int => Point): Boolean = {
    val a = trans(ai)
    val b = trans(bi)
    val c = trans(ci)
    (a.x - c.x) * (b.y - c.y) - (a.y - c.y) * (b.x - c.x) > 0
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
    abc match {
      case (a,b,c) =>
        det3(a.x-d.x, a.y-d.y, pow(a.x-d.x,2) + pow(a.y-d.y,2),
             b.x-d.x, b.y-d.y, pow(b.x-d.x,2) + pow(b.y-d.y,2),
             c.x-d.x, c.y-d.y, pow(c.x-d.x,2) + pow(c.y-d.y,2)) > 0
    }
  }

  def inCircle(abc: (Int, Int, Int), di: Int)(implicit trans: Int => Point): Boolean = {
    abc match {
      case (ai,bi,ci) => {
        val a = trans(ai)
        val b = trans(bi)
        val c = trans(ci)
        val d = trans(di)
        det3(a.x-d.x, a.y-d.y, pow(a.x-d.x,2) + pow(a.y-d.y,2),
             b.x-d.x, b.y-d.y, pow(b.x-d.x,2) + pow(b.y-d.y,2),
             c.x-d.x, c.y-d.y, pow(c.x-d.x,2) + pow(c.y-d.y,2)) > 0
      }
    }
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

case class Delaunay(verts: Array[Point]) {
  val triangles = Map.empty[(Int,Int,Int),HalfEdge[Int,Point]]
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

  def triangleIterator() = triangles.valuesIterator

  // TODO: Make sure to check for duplicate points!!
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
  }.toArray

  def showBoundingLoop[T](base: HalfEdge[Int,T]) = {
    var e = base
    var l: List[HalfEdge[Int,T]] = Nil
    //log.debug("Bounding loop: ")
    while (!l.contains(e)) {
      //log.debug(s"    $e ")
      l = l :+ e
      e = e.next
    }
    l
  }


  def triangulate(lo: Int, hi: Int): HalfEdge[Int,Point] = {
    // Implementation follows Guibas and Stolfi, ACM Transations on Graphics, vol. 4(2), 1985, pp. 74--123
    val n = hi - lo + 1
    implicit val trans = { i: Int => verts(i) }
    import Predicates._
    
    n match {
      case 2 => {
        //log.debug(s"Creating single edge: (${vIx(lo)},${vIx(hi)})")
        val e = HalfEdge[Int,Point](vIx(lo), vIx(hi))
        faceIncidentToVertex += (e.vert -> e, e.src -> e)
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
              //showBoundingLoop(base.flip)
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
              //showBoundingLoop(base.flip)
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
          //showBoundingLoop(base.flip)
        }

        base.flip.next
      }
    }
  }

  val boundary = triangulate(0, vIx.length-1)

}

// val dt = new Delaunay(List(Point(0,0),Point(3,1),Point(7,5),Point(4,7),Point(8,3),Point(1,3)).toArray)
// val t = dt.triangulate(0,5)
// val m = scala.collection.Map(0 -> Point(0,0), 1 -> Point(3,1), 2 -> Point(7,5), 3 -> Point(4,7), 4 -> Point(8,3), 5 -> Point(1,3))
