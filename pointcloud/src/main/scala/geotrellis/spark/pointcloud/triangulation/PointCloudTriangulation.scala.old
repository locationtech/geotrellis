package geotrellis.spark.pointcloud.triangulation

import io.pdal.PointCloud
import geotrellis.raster._
import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}
import geotrellis.vector._
import geotrellis.vector.voronoi._
import geotrellis.vector.io._
import scala.math.{abs, pow}

import scala.annotation.tailrec
import scala.collection.mutable

/**
  * A catalog of the triangles produced by the Delaunay triangulation as a Map from (Int,Int,Int)
  * to an integer index of an edge in a HalfEdgeTable. This class is optimized for
  * rasterization of Z values. Each key in the triangle map is a triplet of indices of
  * points in the pointSet (a, b, c), a<b<c, and the values are the halfedge index
  * of an inner halfedge of the triangle.
  */
class PointCloudTriangulation(
  pointSet: DelaunayPointSet,
  val triangleMap: TriangleMap,
  halfEdgeTable: HalfEdgeTable,
  boundary: Int,
  isLinear: Boolean
) {
  import pointSet.{getX, getY, getZ}

  def trianglePoints =
    triangleMap
      .triangleVertices
      .map { case (v1, v2, v3) =>
        (
          (getX(v1), getY(v1), getZ(v1)),
          (getX(v2), getY(v2), getZ(v2)),
          (getX(v3), getY(v3), getZ(v3))
        )
      }

  def rasterize(tile: MutableArrayTile, re: RasterExtent): Unit = {
    val pcp = new PointSetPredicates(pointSet, halfEdgeTable)
    import pcp._
    import halfEdgeTable._

    val w = re.cellwidth
    val h = re.cellheight
    val cols = re.cols
    val rows = re.rows
    val Extent(exmin, eymin, exmax, eymax) = re.extent

    def rasterizeTriangle(tri: Int): Unit = {

      val e1 = tri
      val v1 = getVert(e1)
      val v1x = getX(v1)
      val v1y = getY(v1)
      val v1z = getZ(v1)
      val s1x = getX(getSrc(e1))
      val s1y = getY(getSrc(e1))

      val e2 = getNext(tri)
      val v2 = getVert(e2)
      val v2x = getX(v2)
      val v2y = getY(v2)
      val v2z = getZ(v2)
      val s2x = getX(getSrc(e2))
      val s2y = getY(getSrc(e2))

      val e3 = getNext(getNext(tri))
      val v3 = getVert(e3)
      val v3x = getX(v3)
      val v3y = getY(v3)
      val v3z = getZ(v3)
      val s3x = getX(getSrc(e3))
      val s3y = getY(getSrc(e3))

      val determinant =
        (v2y - v3y) * (v1x - v3x) + (v3x - v2x) * (v1y - v3y)

      val ymin =
        math.min(v1y, math.min(v2y, v3y))
      val ymax =
        math.max(v1y, math.max(v2y, v3y))

      val scanrow0 = math.max(math.ceil((ymin - eymin) / h - 0.5), 0)
      var scany = eymin + scanrow0 * h + h / 2
      while (scany < eymax && scany < ymax) {
        // get x at y for edge
        var xmin = Double.MinValue
        var xmax = Double.MaxValue

        if(s1y != v1y) {
          val src = getSrc(e1)
          val dest = getVert(e1)
          val t = (scany - v1y) / (s1y - v1y)
          val xAtY1 = v1x + t * (s1x - v1x)

          if(v1y < s1y) {
            // Lefty
            if(xmin < xAtY1) { xmin = xAtY1 }
          } else {
            // Rigty
            if(xAtY1 < xmax) { xmax = xAtY1 }
          }
        }

        if(s2y != v2y) {
          val src = getSrc(e1)
          val dest = getVert(e1)
          val t = (scany - v2y) / (s2y - v2y)
          val xAtY2 = v2x + t * (s2x - v2x)

          if(v2y < s2y) {
            // Lefty
            if(xmin < xAtY2) { xmin = xAtY2 }
          } else {
            // Rigty
            if(xAtY2 < xmax) { xmax = xAtY2 }
          }
        }

        if(s3y != v3y) {
          val src = getSrc(e3)
          val dest = getVert(e3)
          val t = (scany - v3y) / (s3y - v3y)
          val xAtY3 = v3x + t * (s3x - v3x)

          if(v3y < s3y) {
            // Lefty
            if(xmin < xAtY3) { xmin = xAtY3 }
          } else {
            // Rigty
            if(xAtY3 < xmax) { xmax = xAtY3 }
          }
        }

        val scancol0 = math.max(math.ceil((xmin - exmin) / w - 0.5), 0)
        var scanx = exmin + scancol0 * w + w / 2
        while (scanx < exmax && scanx < xmax) {
          val col = ((scanx - exmin) / w).toInt
          val row = ((eymax - scany) / h).toInt
          if(0 <= col && col < cols &&
             0 <= row && row < rows) {

            val z = {

              val lambda1 =
                ((v2y - v3y) * (scanx - v3x) + (v3x - v2x) * (scany - v3y)) / determinant

              val lambda2 =
                ((v3y - v1y) * (scanx - v3x) + (v1x - v3x) * (scany - v3y)) / determinant

              val lambda3 = 1.0 - lambda1 - lambda2

              lambda1 * v1z + lambda2 * v2z + lambda3 * v3z
            }

            tile.setDouble(col, row, z)
          }

          scanx += w
        }

        scany += h
      }
    }

    triangleMap.triangles.foreach(rasterizeTriangle)
  }
}

//
// pointCloud <- hold the points that can be accessed through point indices.
//
// sortedIndexes <- Array[Int], whose values are point indices, sorted firsby X and then by Y
//
// halfEdges <- Array[Int] of triples:
//      [i,e1,e2] where i is the vertex of the halfedge (where the half edge 'points' to)
//                      e1 is the halfedge index of the flip of the halfedge
//                         (the halfedge that points to the source)
//                      e2 is the halfedge index of the next half edge
//                         (counter-clockwise of the triangle)
//
// triangles  <- mutable.Map[(Int, Int, Int), Int]
//                     where each key is a triplet of point indices (a, b, c), a<b<c,
//                     and the values are the halfedge index of an inner index of
//                     the triangle
//

object PointCloudTriangulation {
  case class TriangulationResult(halfEdgeIndex: Int, isLinear: Boolean)

  def apply(pointSet: DelaunayPointSet): PointCloudTriangulation = {
    import pointSet.{getX, getY}

    val halfEdgeTable = new HalfEdgeTable(pointSet.length * 25)
    import halfEdgeTable._

    val pcp = new PointSetPredicates(pointSet, halfEdgeTable)
    import pcp._

    val triangleMap: TriangleMap = new TriangleMap(halfEdgeTable)
    import triangleMap._

    def distinctPoints(lst: List[Int]): List[Int] = {
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

    def advance(e0: Int): Int = {
      var e = getNext(e0)
      while (!isCorner(e))
        e = getNext(e)
      e
    }
    def reverse(e0: Int): Int = {
      var e = getPrev(e0)
      while (!isCorner(e))
        e = getPrev(e)
      e
    }
    def advanceIfNotCorner(e0: Int): Int = {
      var e = e0
      while (!isCorner(e))
        e = getNext(e)
      e
    }

    val sortedVs = {
      val s =
        (0 until pointSet.length).toList.sortWith{
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

      distinctPoints(s)
        .toArray
    }

    /** Recursive triangulation function.
      *
      * @param  lo    The index into sortedVs that represents the left triangulation.
      * @param  hi    The index into sortedVs that represents the right triangulation.
      */
    def triangulate(lo: Int, hi: Int): TriangulationResult = {
      // Implementation follows Guibas and Stolfi, ACM Transations on Graphics, vol. 4(2), 1985, pp. 74--123
      val n = hi - lo + 1

      n match {
        case 1 =>
          throw new IllegalArgumentException("Cannot triangulate a point set of size less than 2")

        case 2 =>
          val e = createHalfEdges(sortedVs(lo), sortedVs(hi))
          TriangulationResult(e, true)

        case 3 =>
          val v1 = sortedVs(lo)
          val v2 = sortedVs(lo + 1)
          val v3 = sortedVs(lo + 2)

          if (isCCW(v1, v2, v3)) {
            val e = createHalfEdges(v1, v2, v3)
            val p = getPrev(e)
            insertTriangle(v1, v2, v3, p)
            TriangulationResult(p, false)
          } else if(isCCW(v1, v3, v2)) {
            val e = createHalfEdges(v1, v3, v2)
            insertTriangle(v1, v3, v2, e)
            TriangulationResult(e, false)
          } else {
            // Linear case
            val e1 = createHalfEdges(v1, v2)
            val e2 = createHalfEdges(v2, v3)
            val e2Flip = getFlip(e2)

            setNext(e1, e2)
            setNext(e2Flip, getFlip(e1))
            TriangulationResult(e2Flip, true)
          }

        case _ => {
          val med = (hi + lo) / 2
          val TriangulationResult(left0, isLeftLinear) = triangulate(lo, med)
          val TriangulationResult(right0, isRightLinear) = triangulate(med + 1, hi)

          var right = advance(right0)
          var left =
            if(isRightLinear && abs(getX(getSrc(left0)) - getX(getSrc(right))) < EPSILON ) {
              // In the case where right is linear, left is not, and the right
              // line is on the same vertical as the border edge of left, we do not
              // want to advance the left border to the bottom, and instead
              // want the top corner of the left border.
              reverse(left0)
            } else { advance(left0) }

          if (isLeftLinear) {
            while(
              getX(getSrc(left)) - getX(getVert(left)) < -EPSILON ||
              (
                abs(getX(getSrc(left)) - getX(getVert(left))) < EPSILON &&
                getY(getSrc(left)) - getY(getVert(left)) < -EPSILON
              )
            ) { left = advance(left) }
          }

          if (isRightLinear) {
            while(
              getX(getSrc(right)) - getX(getVert(getNext(getFlip(right)))) > EPSILON ||
              (
                abs(getX(getSrc(right)) - getX(getVert(getNext(getFlip(right))))) < EPSILON &&
                getY(getSrc(right)) - getY(getVert(getNext(getFlip(right)))) > EPSILON
              )
            ) { right = advance(right)}
          }

          // compute the lower common tangent of left and right
          var continue = true
          var base: Int = createHalfEdges(getSrc(right), getSrc(left))

          while(continue) {
            if(isLeftOf(base, getVert(left))) {
              // left points to a vertex that is to the left of
              // base, so move base to left.next
              left = advance(left)
              setVert(base, getSrc(left))
            } else if(!isRightLinear && !isRightOf(base, getVert(right))) {
              // right points to a point that is left of base,
              // so keep walking right
              right = advance(right)
              setSrc(base, getSrc(right))
            } else if(!isLeftLinear && !isRightOf(base, getSrc(getPrev(left)))) {
              // Left's previous source is left of base,
              // so this base would break convexity. Move
              // back to previous left.
              left = reverse(left)
              setVert(base, getSrc(left))
            } else if(isLeftOf(base, getSrc(getPrev(right)))) {
              // Right's previous source is left ofbase,
              // so this base would break convexity. Move
              // back to previous right.
              right = reverse(right)
              setSrc(base, getSrc(right))
            } else {
              continue = false
            }
          }

          setNext(base, left)
          setNext(getFlip(base), right)
          setNext(getPrev(left), getFlip(base))
          setNext(getPrev(right), base)

          // If linear joins to linear, check that the current state
          // isn't already done (linear result)
          if(isLeftLinear && isRightLinear) {
            val b0 = getSrc(base)
            val b1 = getVert(base)
            val l = getVert(getNext(base))
            val r = getVert(getNext(getFlip(base)))
            if (isCollinear(b0, b1, l)  && isCollinear(b0, b1, r)) {
              return TriangulationResult(getFlip(base), true)
            }
          }

          continue = true
          while(continue) {
            var lcand = rotCCWSrc(getFlip(base))
            var rcand = rotCWSrc(base)

            // Find left side candidate edge for extending the fill triangulation
            if(isCCW(getVert(lcand), getVert(base), getSrc(base))) {
              while(
                inCircle(
                  getVert(base),
                  getSrc(base),
                  getVert(lcand),
                  getVert(rotCCWSrc(lcand))
                )
              ) {
                val e = rotCCWSrc(lcand)
                deleteTriangle(lcand)
                setNext(rotCCWDest(lcand), getNext(lcand))
                setNext(getPrev(lcand), getNext(getFlip(lcand)))
                lcand = e
              }
            }

            // Find right side candidate edge for extending the fill triangulation
            if(isCCW(getVert(rcand), getVert(base), getSrc(base))) {
              while(
                inCircle(
                  getVert(base),
                  getSrc(base),
                  getVert(rcand),
                  getVert(rotCWSrc(rcand))
                )
              ) {
                val e = rotCWSrc(rcand)
                deleteTriangle(getFlip(rcand))
                setNext(getFlip(base), rotCWSrc(rcand))
                setNext(rotCCWDest(rcand), getNext(rcand))
                rcand = e
              }
            }

            if(
              !isCCW(getVert(lcand), getVert(base), getSrc(base)) &&
              !isCCW(getVert(rcand), getVert(base), getSrc(base))
            ) {
              // no further Delaunay triangles to add
              continue = false
            } else {
              if (!isCCW(getVert(lcand), getVert(base), getSrc(base)) ||
                  (
                    isCCW(getVert(rcand), getVert(base), getSrc(base)) &&
                    inCircle(getVert(lcand), getSrc(lcand), getSrc(rcand), getVert(rcand))
                  )
              ) {
                // form new triangle from rcand and base
                val e = createHalfEdges(getVert(rcand), getVert(base))
                setNext(getFlip(e), getNext(rcand))
                setNext(e, getFlip(base))
                setNext(rcand, e)
                setNext(getFlip(lcand), getFlip(e))
                base = e
              } else {
                // form new triangle from lcand and base
                val e = createHalfEdges(getSrc(base), getVert(lcand))
                setNext(rotCCWDest(lcand), getFlip(e))
                setNext(e, getFlip(lcand))
                setNext(getFlip(e), rcand)
                setNext(getFlip(base), e)
                base = e
              }

              insertTriangle(base)
            }
          }


          TriangulationResult(getNext(getFlip(base)), false)
        }
      }
    }

    val TriangulationResult(boundary, isLinear) = triangulate(0, sortedVs.length - 1)

    new PointCloudTriangulation(pointSet, triangleMap, halfEdgeTable, boundary, isLinear)
  }
}
