package geotrellis.vector.triangulation

import scala.annotation.tailrec

case class DelaunayTriangulation(verts: DelaunayPointSet, navigator: HalfEdgeTable)
{
  implicit val nav = navigator
  implicit val trans = verts.getCoordinate(_)

  val triangles = new TriangleMap

  def distinctPoints(lst: List[Int]): List[Int] = {
    import verts._

    @tailrec def dpInternal(l: List[Int], acc: List[Int]): List[Int] = {
      l match {
        case Nil => acc
        case i :: Nil => i :: acc
        case i :: rest => {
          val chunk = rest.takeWhile{j => getX(j) <= getX(i) + 1e-8}
          if (chunk.exists { j =>
            val (xj, yj) = (getX(j), getY(j))
              val (xi, yi) = (getX(i), getY(i))
                val dx = xj - xi
                            val dy = yj - yi
                            math.sqrt((dx * dx) + (dy * dy)) < 1e-10
                          }) {
            dpInternal(rest, acc)
          } else {
            dpInternal(rest, i :: acc)
          }
        }
      }
    }

    dpInternal(lst, Nil).reverse
  }

  val sortedVs = {
    import verts._
    val s =
      (0 until verts.length).toList.sortWith{
        (i1,i2) => {
          val x1 = getX(i1)
          val x2 = getX(i2)
          if (x1 < x2) {
            true
          } else {
            if (x1 > x2) {

              false
            } else {
              getY(i1) < getY(i2)
            }
          }
        }
      }
    println("Finding distinct points")
    distinctPoints(s)
    .toArray
  }

  def triangulate(lo: Int, hi: Int): (Int, Boolean) = {
    // Implementation follows Guibas and Stolfi, ACM Transations on Graphics, vol. 4(2), 1985, pp. 74--123
    import nav._
    import Predicates._

    val n = hi - lo + 1

    n match {
      case 1 =>
        throw new IllegalArgumentException("Cannot triangulate a point set of size less than 2")

      case 2 =>
        val e = createHalfEdges(sortedVs(lo), sortedVs(hi))
        (e, true)

      case 3 =>
        val v1 = sortedVs(lo)
        val v2 = sortedVs(lo + 1)
        val v3 = sortedVs(lo + 2)

        if (isCCW(v1, v2, v3)) {
          val e = createHalfEdges(v1, v2, v3)
          val p = getPrev(e)
          triangles += (v1, v2, v3) -> p
          (p, false)
        } else if(isCCW(v1, v3, v2)) {
          val e = createHalfEdges(v1, v3, v2)
          triangles += (v1, v3, v2) -> e
          (e, false)
        } else {
          // Linear case
          val e1 = createHalfEdges(v1, v2)
          val e2 = createHalfEdges(v2, v3)
          val e2Flip = getFlip(e2)

          setNext(e1, e2)
          setNext(e2Flip, getFlip(e1))
          (e2Flip, true)
        }

      case _ => {
        val med = (hi + lo) / 2
        var (left, isLeftLinear) = triangulate(lo,med)
        var (right, isRightLinear) = triangulate(med+1,hi)

        DelaunayStitcher.merge(left, isLeftLinear, right, isRightLinear, triangles)
      }
    }
  }

  val (boundary, isLinear) = triangulate(0, sortedVs.length - 1)
}

object DelaunayTriangulation {
  def apply(verts: DelaunayPointSet) = {
    new DelaunayTriangulation(verts, new HalfEdgeTable(2*(3*verts.length - 6)))
  }
}
