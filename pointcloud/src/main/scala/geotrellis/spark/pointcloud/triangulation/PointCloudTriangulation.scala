
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

trait DelaunayPointSet {
  def length: Int
  def getX(i: Int): Double
  def getY(i: Int): Double
  def getZ(i: Int): Double
}

object DelaunayPointSet {

  implicit def pointCloudToDelaunayPointSet(pointCloud: PointCloud): DelaunayPointSet =
    new DelaunayPointSet {
      def length = pointCloud.length
      def getX(i: Int) = pointCloud.getX(i)
      def getY(i: Int) = pointCloud.getY(i)
      def getZ(i: Int) = pointCloud.getZ(i)
    }

  implicit def lightPointArrayToDelaunayPointSet(lightPoints: Array[LightPoint]) =
    new DelaunayPointSet {
      def length = lightPoints.length
      def getX(i: Int) = lightPoints(i).x
      def getY(i: Int) = lightPoints(i).y
      def getZ(i: Int) = lightPoints(i).z
    }
}

/**
  * A catalog of the triangles produced by the Delaunay triangulation as a Map from (Int,Int,Int)
  * to HalfEdge[Int,LightPoint].  All integers are indices of points in verts, while the LightPoints describe
  * the center of the circumscribing circle for the triangle.  The Int triples give the triangle
  * vertices in counter-clockwise order, starting from the lowest-numbered vertex.
  */
class PointCloudTriangulation(
  pointSet: DelaunayPointSet,
  triangleMap: TriangleMap,
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

    def xAtYForEdge(yval: Double)(e: Int): Double = {
      val src = getSrc(e)
      val dest = getVert(e)
      val t = (yval - getY(dest)) / (getY(src) - getY(dest))
      getX(dest) + t * (getX(src) - getX(dest))
    }

    def rasterizeTriangle(tri: Int): Unit = {
      if (!isCCW(getVert(tri), getVert(getNext(tri)), getVert(getNext(getNext(tri))))) {
        println(s"Triangle ${(getVert(tri), getVert(getNext(tri)), getVert(getNext(getNext(tri))))} does not have a CCW winding!")
        return ()
      }

      val v1 = getVert(tri)
      val v1x = getX(v1)
      val v1y = getY(v1)
      val v1z = getZ(v1)
      val v2 = getVert(getNext(tri))
      val v2x = getX(v2)
      val v2y = getY(v2)
      val v2z = getZ(v2)
      val v3 = getVert(getNext(getNext(tri)))
      val v3x = getX(v3)
      val v3y = getY(v3)
      val v3z = getZ(v3)

      val determinant =
        (v2y - v3y) * (v1x - v3x) + (v3x - v2x) * (v1y - v3y)

      val es = List(tri, getNext(tri), getNext(getNext(tri)))
      val leftes  = es.filter { e => getY(getVert(e)) < getY(getSrc(e)) }
      val rightes = es.filter { e => getY(getVert(e)) > getY(getSrc(e)) }
//      println(s"LEFTIES $leftes RIGHTES $rightes")
      val ys = es.map { e => getY(getVert(e)) }
      val (ymin, ymax) = (ys.min, ys.max)

      // println (s"Lefts:  ${leftes}")
      // println (s"Rights: ${rightes}")
      // println (s"Y range: ${(ymin, ymax)}")

      val scanrow0 = math.max(math.ceil((ymin - re.extent.ymin) / h - 0.5), 0)
      var scany = re.extent.ymin + scanrow0 * h + h / 2
      while (scany < re.extent.ymax && scany < ymax) {
        val xmin = leftes.map(xAtYForEdge(scany)(_)).max
        val xmax = rightes.map(xAtYForEdge(scany)(_)).min

        val scancol0 = math.max(math.ceil((xmin - re.extent.xmin) / w - 0.5), 0)
        var scanx = re.extent.xmin + scancol0 * w + w / 2
        while (scanx < re.extent.xmax && scanx < xmax) {
          val col = ((scanx - re.extent.xmin) / re.cellwidth).toInt
          val row = ((re.extent.ymax - scany) / re.cellheight).toInt
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

    val triangleMap: TriangleMap = new TriangleMap(halfEdgeTable, pointSet)
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

    var printBase = false
    /** Recursive triangulation function.
      *
      * @param  lo    The index into sortedVs that represents the left triangulation.
      * @param  hi    The index into sortedVs that represents the right triangulation.
      */
    def triangulate(lo: Int, hi: Int): TriangulationResult = {
      // Implementation follows Guibas and Stolfi, ACM Transations on Graphics, vol. 4(2), 1985, pp. 74--123
//      println(s" -- triangulate($lo, $hi)")
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
            //println(s"DELAUNAY2 = CASE 1 = $p = ($v1, $v2, $v3)")
            TriangulationResult(p, false)
          } else if(isCCW(v1, v3, v2)) {
            val e = createHalfEdges(v1, v3, v2)
            insertTriangle(v1, v3, v2, e)
            //println(s"DELAUNAY2 = CASE 2 = $e = ($v1, $v3, $v2)")
            TriangulationResult(e, false)
          } else {
            // Linear case
            val e1 = createHalfEdges(v1, v2)
            val e2 = createHalfEdges(v2, v3)
            val e2Flip = getFlip(e2)

            setNext(e1, e2)
            setNext(e2Flip, getFlip(e1))
            //println(s"DELAUNAY2 = CASE 3 = $e2Flip = ($v1, $v2, $v3)")
            TriangulationResult(e2Flip, true)
          }

        case _ => {
          val med = (hi + lo) / 2
          var TriangulationResult(left, isLeftLinear) = triangulate(lo, med)
          var TriangulationResult(right, isRightLinear) = triangulate(med + 1, hi)

//          println(s"DELAUNAY1 -- LEFT: triangulate($lo, $med) = ${left} RIGHT: triangulate($med, $hi) = ${right}")

//       saveBorder(left, right)

          if (isLeftLinear) {
            while(
              getX(getSrc(left)) - getX(getVert(left)) < -EPSILON ||
              (
                abs(getX(getSrc(left)) - getX(getVert(left))) < EPSILON &&
                getY(getSrc(left)) - getY(getVert(left)) < -EPSILON
              )
            ) { left = getNext(left) }
          }

//          println("ONE")

          if (isRightLinear) {
            while(
              getX(getSrc(right)) - getX(getVert(getNext(getFlip(right)))) > EPSILON ||
              (
                abs(getX(getSrc(right)) - getX(getVert(getNext(getFlip(right))))) < EPSILON &&
                getY(getSrc(right)) - getY(getVert(getNext(getFlip(right)))) > EPSILON
              )
            ) { right = getFlip(getNext(getFlip(right))) }
          }

//          println("TWO")

          // compute the lower common tangent of left and right
          var continue = true
          var base: Int = -1
          var baseCount: Int = 0
//          time("CALCULATING BASE") {
          while(continue) {
            base = createHalfEdges(getSrc(right), getSrc(left))

//            saveBase(base, baseCount)
            baseCount += 1
//            println(s"DELAUNAY2: BASE LOOP B = ${base} R = ${right} L = ${left}")
//            println(s"BASE $base")
            if(isLeftOf(base, getVert(left))) {
              // left points to a vertex that is to the left of
              // base, so move base to left.next
//              println(s"1")
              left = getNext(left)
            } else if(isLeftOf(base, getVert(right))) {
              // right points to a point that is left of base,
              // so keep walking right
//              println(s"2")
              right = getNext(right)
            } else if(isLeftOf(base, getSrc(getPrev(left)))) {
              // Left's previous source is left of base,
              // so this base would break convexity. Move
              // back to previous left.
//              println(s"3")
              left = getPrev(left)
            } else if(isLeftOf(base, getSrc(getPrev(right)))) {
              // Right's previous source is left ofbase,
              // so this base would break convexity. Move
              // back to previous right.
//              println(s"4")
              right = getPrev(right)
            } else if(isCollinear(base, getVert(right)) &&
                      !isRightLinear) {
//              println(s"5")
              // Right is colinear with base, move right forward.
              right = getNext(right)
            } else {
//              println(s"6")
              continue = false
            }
          }
//          }

//          println("THREE")

          //println(s"DELAUNAY2   - Out of base loop. ${base}")
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

//          println("FOUR")

          continue = true
          while(continue) {
            var lcand = rotCCWSrc(getFlip(base))
            var rcand = rotCWSrc(base)

            //println(s"DELAUNAY2:   ~~ STARTWHILE ~~ $lcand $rcand $base ${getVert(lcand)}, ${getVert(base)}, ${getSrc(base)}")
            // Find left side candidate edge for extending the fill triangulation
            if(isCCW(getVert(lcand), getVert(base), getSrc(base))) {
              //println(s"DELAUNAY2:  WHILE 1")
              while(
                inCircle(
                  getVert(base),
                  getSrc(base),
                  getVert(lcand),
                  getVert(rotCCWSrc(lcand))
                )
              ) {
//                println("DELETED TRIANGLE")
                val e = rotCCWSrc(lcand)
                deleteTriangle(lcand)
                setNext(rotCCWDest(lcand), getNext(lcand))
                setNext(getPrev(lcand), getNext(getFlip(lcand)))
                lcand = e
              }
            }

            // Find right side candidate edge for extending the fill triangulation
            if(isCCW(getVert(rcand), getVert(base), getSrc(base))) {
              //println(s"DELAUNAY2:  WHILE 2")
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
              //println(s"DELAUNAY2:  CONT 3")
              if (!isCCW(getVert(lcand), getVert(base), getSrc(base)) ||
                  (
                    isCCW(getVert(rcand), getVert(base), getSrc(base)) &&
                    inCircle(getVert(lcand), getSrc(lcand), getSrc(rcand), getVert(rcand))
                  )
              ) {
                // form new triangle from rcand and base
                //println(s"DELAUNAY2:  ==RCAND BASE $rcand $base")
                val e = createHalfEdges(getVert(rcand), getVert(base))
                setNext(getFlip(e), getNext(rcand))
                setNext(e, getFlip(base))
                setNext(rcand, e)
                setNext(getFlip(lcand), getFlip(e))
                base = e

//                saveBase(base, baseCount, true, "rh")
                baseCount += 1
              } else {
                // form new triangle from lcand and base
                //println(s"DELAUNAY2:  ==LCAND BASE $lcand $base")
                val e = createHalfEdges(getSrc(base), getVert(lcand))
                setNext(rotCCWDest(lcand), getFlip(e))
                setNext(e, getFlip(lcand))
                setNext(getFlip(e), rcand)
                setNext(getFlip(base), e)
                base = e

//                saveBase(base, baseCount, true, "lh")
                baseCount += 1
              }

//              println(s"INSERTING $base")
              insertTriangle(base)
            }
//            println(s" LOOOP $base $lcand $rcand")
          }


          TriangulationResult(getNext(getFlip(base)), false)
        }
      }
    }

    /**
     * Provides a handle on the bounding loop of a Delaunay triangulation.  All of the half-edges
     * reachable by applying next to boundary have no bounding face information.  That is
     * boundary(.next)*.face == None.
     */
    val TriangulationResult(boundary, isLinear) = triangulate(0, sortedVs.length - 1)

    new PointCloudTriangulation(pointSet, triangleMap, halfEdgeTable, boundary, isLinear)
  }
}

class PointSetPredicates(pointSet: DelaunayPointSet, halfEdgeTable: HalfEdgeTable) {
  import geotrellis.vector.voronoi.ShewchuksDeterminant

  import pointSet.{getX, getY}
  import halfEdgeTable._

  def isCCW(v1: Int, v2: Int, v3: Int): Boolean =
    ShewchuksDeterminant.orient2d(
      getX(v1),
      getY(v1),
      getX(v2),
      getY(v2),
      getX(v3),
      getY(v3)
    ) > EPSILON

  def isLeftOf(e: Int, v: Int): Boolean =
    isCCW(v, getSrc(e), getVert(e))

  def isCollinear(e: Int, v: Int): Boolean = {
    val src = getSrc(e)
    val vert = getVert(e)

    isCollinear(src, vert, v)
  }

  def isCollinear(a: Int, b: Int, c: Int): Boolean =
    math.abs(
      ShewchuksDeterminant.orient2d(
        getX(a), getY(a),
        getX(b), getY(b),
        getX(c), getY(c)
      )
    ) < EPSILON

  def inCircle(a: Int, b: Int, c: Int, d: Int): Boolean =
    ShewchuksDeterminant.incircle(
      getX(a), getY(a),
      getX(b), getY(b),
      getX(c), getY(c),
      getX(d), getY(d)
    ) > EPSILON
}

class TriangleMap(halfEdgeTable: HalfEdgeTable, ps: DelaunayPointSet) {
  import halfEdgeTable._
  import ps._ // TODO

  private val _triangles =
    mutable.Map.empty[(Int, Int, Int), Int]

  def triangleVertices =
    _triangles.keys

  def triangles =
    _triangles.values

  private def regularizeTriangleIndex (index: (Int, Int, Int)): (Int, Int, Int) = {
    index match {
      case (a, b, c) if (a < b && a < c) => (a, b, c)
      case (a, b, c) if (b < a && b < c) => (b, c, a)
      case (a, b, c) => (c, a, b)
    }
  }

  def insertTriangle(v1: Int, v2: Int, v3: Int, e: Int): Unit =
    if(v1 < v2 && v1 < v3) { _triangles += (((v1, v2, v3), e)) }
    else if(v2 < v1 && v2 < v3) { _triangles += (((v2, v3, v1), e)) }
    else { _triangles += (((v3, v1, v2), e)) }

  def insertTriangle(e: Int): Unit =
    insertTriangle(getVert(e), getVert(getNext(e)), getVert(getNext(getNext(e))), e)

  def deleteTriangle(v1: Int, v2: Int, v3: Int): Unit =
    if(v1 < v2 && v1 < v3) { _triangles -= ((v1, v2, v3)) }
    else if(v2 < v1 && v2 < v3) { _triangles -= ((v2, v3, v1)) }
    else { _triangles -= ((v3, v1, v2)) }

  def deleteTriangle(e: Int): Unit =
    deleteTriangle(getVert(e), getVert(getNext(e)), getVert(getNext(getNext(e))))
}
