package geotrellis.spark.pointcloud.triangulation

import io.pdal.PointCloud
import geotrellis.spark.buffer.Direction
import geotrellis.spark.pointcloud._
import geotrellis.raster._
import geotrellis.raster.triangulation._
import geotrellis.util.Constants.{DOUBLE_EPSILON => EPSILON}
import geotrellis.vector._
import geotrellis.vector.triangulation._
import geotrellis.vector.io._

import com.vividsolutions.jts.algorithm.distance.{DistanceToPoint, PointPairDistance}

import scala.math.{abs, pow}
import scala.annotation.tailrec
import scala.collection.mutable

case class StitchKey(dir: Direction, idx: Int) extends Ordered[StitchKey] {
  import StitchKey.ord

  def compare(other: StitchKey): Int =
    ord.compare(this, other)
}

object StitchKey {
  import Direction._

  final val directionToInt: Map[Direction, Int] =
    Map(
      (TopLeft, 1),
      (Top, 2),
      (TopRight, 3),
      (Left, 4),
      (Center, 5),
      (Right, 6),
      (BottomLeft, 7),
      (Bottom, 8),
      (BottomRight, 9)
    )

  implicit val ord: Ordering[StitchKey] =
    Ordering.by[StitchKey, (Int, Int)] { k: StitchKey =>
      (directionToInt(k.dir), k.idx)
    }

  implicit def vkeyConvert(di: (Direction, Int)): StitchKey =
    StitchKey(di._1, di._2)
}

case class HalfEdgeBoundingMesh[V](
  extent: Extent,
  boundary: HalfEdge[V, Unit],
  triangles: Map[(V, V, V), HalfEdge[V, Unit]],
  isLinear: Boolean,
  points: mutable.Map[V, Point3D]
) {
  def rasterize(tile: MutableArrayTile, re: RasterExtent): Unit = {
    val w = re.cellwidth
    val h = re.cellheight
    val cols = re.cols
    val rows = re.rows
    val Extent(exmin, eymin, exmax, eymax) = re.extent

    def rasterizeTriangle(tri: HalfEdge[V, Unit]): Unit = {

      val e1 = tri
      val v1 = e1.vert
      val p1 = points(v1)
      val v1x = p1.x
      val v1y = p1.y
      val v1z = p1.z
      val sp1 = points(e1.src)
      val s1x = sp1.x
      val s1y = sp1.y

      val e2 = tri.next
      val v2 = e2.vert
      val p2 = points(v2)
      val v2x = p2.x
      val v2y = p2.y
      val v2z = p2.z
      val sp2 = points(e2.src)
      val s2x = sp2.x
      val s2y = sp2.y

      val e3 = tri.next.next
      val v3 = e3.vert
      val p3 = points(v3)
      val v3x = p3.x
      val v3y = p3.y
      val v3z = p3.z
      val sp3 = points(e3.src)
      val s3x = sp3.x
      val s3y = sp3.y

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

    triangles.map(_._2).foreach(rasterizeTriangle)
  }
}

case class BoundingMesh(
  extent: Extent,
  halfEdgeTable: HalfEdgeTable,
  boundary: Int,
  triangles: Map[(Int, Int, Int), Int],
  isLinear: Boolean,
  points: mutable.Map[Int, Point3D]
) {
//   def showTriangles(prefix: String = ""): Unit = {
//     import halfEdgeTable._
// //    val Extent(xmin, ymin, xmax, ymax) = Extent(596641.16, 7591125.83, 596641.51, 7591366.46)
// //    val Extent(xmin, ymin, xmax, ymax) = Extent(596641.49, 7591125.83, 596641.51, 7591354.24)
//     // var (xmin, ymin, xmax, ymax) =
//     //   (Double.MaxValue, Double.MaxValue, Double.MinValue, Double.MinValue)

//     // triangles.keys.foreach { case (i1, i2, i3) =>
//     //   val p1 = verts(i1)
//     //   val p2 = verts(i2)
//     //   val p3 = verts(i3)

//     //   xmin = math.min(xmin, math.min(p1.x, math.min(p2.x, p3.x)))
//     //   ymin = math.min(ymin, math.min(p1.y, math.min(p2.y, p3.y)))
//     //   xmax = math.max(xmax, math.max(p1.x, math.max(p2.x, p3.x)))
//     //   ymax = math.max(ymax, math.max(p1.y, math.max(p2.y, p3.y)))
//     // }
//     // println(s"${Extent(xmin, ymin, xmax, ymax)}")

//     // def norm(p: LightPoint): LightPoint = {
//     //   LightPoint(
//     //     p.x,
//     //     (((p.y - ymin) / (ymax - ymin)) * (xmax - xmin)) + ymin
//     //   )
//     // }

//     val gc =
//       GeometryCollection(polygons =
//         triangles.keys.map { case (i1, i2, i3) =>
//           val p1 = Point3D(points(i1).x, points(i1).y)
//           val p2 = Point3D(points(i2).x, points(i2).y)
//           val p3 = Point3D(points(i3).x, points(i3).y)

//           val (z1, z2, z3) = (p1.z, p2.z, p3.z)
//           Polygon(Line(p1.toPoint, p2.toPoint, p3.toPoint, p1.toPoint))
//         }.toSeq)

//     // println(gc.toWKT)
//     // write(
//     //   s"/Users/rob/proj/jets/delaunay-98/STITCH-${prefix}.wkt",
//     //   gc.toWKT
//     // )

//   }
}

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
  //TODO
  // def write(path: String, txt: String): Unit = {
  //   import java.nio.file.{Paths, Files}
  //   import java.nio.charset.StandardCharsets

  //   Files.write(Paths.get(path), txt.getBytes(StandardCharsets.UTF_8))
  // }


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

  def boundingMesh(boundingExtent: Extent): BoundingMesh = {
    val pcp = new PointSetPredicates(pointSet, halfEdgeTable)
    import pcp._
    import halfEdgeTable._

    val r = new HalfEdgeTable(pointSet.length / 2)

    type ResultEdge = Int

    val _triangles =
      collection.mutable.Map.empty[(Int, Int, Int), ResultEdge]

    val points = mutable.Map[Int, Point3D]()

    def addPoint(v: Int): Unit =
      points.getOrElseUpdate(v, Point3D(getX(v), getY(v), getZ(v)))

    val outerEdges = collection.mutable.Set[(Int, Int)]()

    val boundingEdge = {
      def lookupTriangle(tri: Int): Option[ResultEdge] = {
        val normalized =
          TriangleMap.regularizeTriangleIndex(
            getVert(tri), getVert(getNext(tri)), getVert(getNext(getNext(tri)))
          )
        _triangles.get(normalized) match {
          case Some(base) => {
            var e = base
            do {
              //println(s"YUP THIS IS IT $e")
              if (r.getVert(e) == getVert(tri)) {
                return Some(e)
              }
              e = r.getNext(e)
            } while (e != base)

            Some(base)
          }
          case None => {
            None
          }
        }
      }

      def copyConvertEdge(e: Int): ResultEdge = {
        addPoint(getSrc(e))
        addPoint(getVert(e))
        r.createHalfEdges(getSrc(e), getVert(e))
      }

      def copyConvertLinearBound(): ResultEdge = {
        //println("copyConvertLinearB")
        val hes = collection.mutable.Map.empty[(Int, Int), ResultEdge]
        var e = boundary
        do {
          val edge = r.createHalfEdge(getVert(e))
          addPoint(getVert(e))
          hes += (getSrc(e), getVert(e)) -> edge
          e = getNext(e)
        } while (e != boundary)

        do {
          val edge = hes((getSrc(e), getVert(e)))
          val flip = hes((getVert(e), getSrc(e)))
          r.setFlip(edge, flip)
          r.setFlip(flip, edge)
          r.setNext(edge, hes((getVert(e), getVert(getNext(e)))))
          e = getNext(e)
        } while (e != boundary)

        hes((getSrc(boundary), getVert(boundary)))
      }

      def copyConvertBoundingLoop(orig: Int, newBound: ResultEdge): ResultEdge = {
        //println("copyConvertBoundingLoop")
        // Record orig as a outer bound
        outerEdges += ((getSrc(orig), getVert(orig)))

        var copy =
          if (orig == boundary) newBound else copyConvertEdge(orig)

        if (getNext(orig) != boundary) {
          r.setNext(copy, copyConvertBoundingLoop(getNext(orig), newBound))
        } else {
          r.setNext(copy, newBound)
        }
        r.setNext(r.getFlip(r.getNext(copy)), r.getFlip(copy))
        copy
      }

      def copyConvertTriangle(tri: Int): ResultEdge = {
        //println(s"COPY CONV BEG")
        val copy =
          r.getFlip(
            r.getNext(
              r.createHalfEdges(getVert(tri), getVert(getNext(tri)), getVert(getNext(getNext(tri))))
            )
          )

        addPoint(getVert(tri))
        addPoint(getVert(getNext(tri)))
        addPoint(getVert(getNext(getNext(tri))))

        val idx =
          TriangleMap.regularizeTriangleIndex(
            r.getVert(copy),
            r.getVert(r.getNext(copy)),
            r.getVert(r.getNext(r.getNext(copy)))
          )

        // if(idx == TriangleMap.regularizeTriangleIndex(2683,2690,2684)) {
        //   // println("ORIGINAL HIT")
        //     val (px1, px2, px3) =
        //       (
        //         points(r.getVert(copy)),
        //         points(r.getVert(r.getNext(copy))),
        //         points(r.getVert(r.getNext(r.getNext(copy))))
        //       )
        //     println(Polygon(px1.toPoint, px2.toPoint, px3.toPoint, px1.toPoint).toWKT)
        // }

        _triangles += idx -> copy
        // println(s"COPY CONV END")
        // print(s" ADDIN TRIANGLE ${idx}: ")
        // showBoundingLoop(tri)
        // println("      ADD +++++++++")
        // r.showBoundingLoop(copy)
        copy
      }

      // TODO: Compute the distance from a point to an extent faster.
      def circumcircleLeavesExtent(extent: Extent)(tri: Int): Boolean = {
        //println(s"CIRCUMVAVA")
        val center =
          circleCenter(getVert(tri), getVert(getNext(tri)), getVert(getNext(getNext(tri))))
        val radius = center.distance2D(Point3D(getX(getVert(tri)), getY(getVert(tri))))
        val ppd = new PointPairDistance
        val closest =
          DistanceToPoint.computeDistance(extent.toPolygon.jtsGeom, center.toPoint.jtsGeom.getCoordinate, ppd)

        //println(s"CIRCUMVAVA BAAA")
        ppd.getDistance < radius
      }

      def join(tri1: Int, tri2: Int): Unit = {
        // val v11 = r.getSrc(tri1)
        // val v12 = r.getVert(r.getNext(tri1))
        // val v13 = r.getVert(r.getNext(r.getNext(tri1)))

        // val v21 = r.getSrc(tri2)
        // val v22 = r.getVert(r.getNext(tri2))
        // val v23 = r.getVert(r.getNext(r.getNext(tri2)))

        r.joinTriangles(tri1, tri2)

        // val v31 = r.getSrc(tri1)
        // val v32 = r.getVert(r.getNext(tri1))
        // val v33 = r.getVert(r.getNext(r.getNext(tri1)))

        // val v41 = r.getSrc(tri2)
        // val v42 = r.getVert(r.getNext(tri2))
        // val v43 = r.getVert(r.getNext(r.getNext(tri2)))

        // if(
        //   TriangleMap.regularizeTriangleIndex(v11, v12, v13)._1 == (2683,2690,2684)._1 ||
        //   TriangleMap.regularizeTriangleIndex(v11, v12, v13)._1 == (2683,2690,2684)._2 ||
        //   TriangleMap.regularizeTriangleIndex(v11, v12, v13)._1 == (2683,2690,2684)._3 ||
        //   TriangleMap.regularizeTriangleIndex(v21, v22, v23)._1 == (2683,2690,2684)._1 ||
        //   TriangleMap.regularizeTriangleIndex(v21, v22, v23)._1 == (2683,2690,2684)._2 ||
        //   TriangleMap.regularizeTriangleIndex(v21, v22, v23)._1 == (2683,2690,2684)._3
        // ) {
        //     val (px1, px2, px3) =
        //       (
        //         points(v31),
        //         points(v32),
        //         points(v33)
        //       )
        // println(s"JOIN ${(v11, v12, v13)}")
        //     println(Polygon(px1.toPoint, px2.toPoint, px3.toPoint, px1.toPoint).toWKT)
        // println(s"JOINTO ${(v21, v22, v23)}")
        //     val (ox1, ox2, ox3) =
        //       (
        //         points(v41),
        //         points(v42),
        //         points(v43)
        //       )
        //     println(Polygon(ox1.toPoint, ox2.toPoint, ox3.toPoint, ox1.toPoint).toWKT)
        // }

        // if(TriangleMap.regularizeTriangleIndex(v11, v12, v13) !=
        //   TriangleMap.regularizeTriangleIndex(v31, v32, v33)) {
        //   sys.error(s"JOIN CHANGED (IN) ${(v11, v12, v13)} to ${(v31, v32, v33)}")
        // }

        // if(TriangleMap.regularizeTriangleIndex(v21, v22, v23) !=
        //   TriangleMap.regularizeTriangleIndex(v41, v42, v43)) {
        //   sys.error(s"JOIN CHANGED (OUT) ${(v21, v22, v23)} to ${(v41, v42, v43)}")
        // }
      }

      def recursiveAddTris(e0: Int, opp0: ResultEdge): Unit = {
        //println("recursiveAddTris")
        val workQueue = collection.mutable.Queue( (e0, opp0) )

        while (!workQueue.isEmpty) {
          val (e, opp) = workQueue.dequeue
          // println(s"     WORKING ON:")
          // showBoundingLoop(e)
          // r.showBoundingLoop(opp)
          // println(s"=======")
          val isOuterEdge = outerEdges.contains((getSrc(e), getVert(e)))
          val isFlipOuterEdge = {
            val flip = r.getFlip(opp)
            r.getVert(r.getNext(r.getNext(r.getNext(flip)))) != r.getVert(flip)
          }
          if (!isOuterEdge && isFlipOuterEdge) {
            //  opp.flip must be boundary edge (isn't interior to triangle)
            lookupTriangle(e) match {
              case Some(tri) =>
                // println(s"    --- FOUND TRIANGLE:")
                // r.showBoundingLoop(tri)
                join(opp, tri)
                // r.joinTriangles(opp, tri)
                // println(s"    JOIN FOUND TRIANGLE")
                // r.showBoundingLoop(r.getFlip(r.getNext(tri)))
                // r.showBoundingLoop(r.getFlip(r.getNext(opp)))
              case None =>
                if (circumcircleLeavesExtent(boundingExtent)(e)) {
                  val tri = copyConvertTriangle(e)
                  // r.showBoundingLoop(tri)
                  // r.joinTriangles(opp, tri)
                  join(opp, tri)

                  workQueue.enqueue( (getFlip(getNext(e)), r.getNext(tri)) )
                  workQueue.enqueue( (getFlip(getNext(getNext(e))), r.getNext(r.getNext(tri))) )
                }
            }
          }
        }
      }

      def copyConvertBoundingTris(): ResultEdge = {
        //println("copyConvertBT")
        val newBound: ResultEdge =
          copyConvertBoundingLoop(boundary, copyConvertEdge(boundary))
        var e = boundary
        var ne = newBound
        val boundingTris = collection.mutable.Set.empty[Int]
        do {
          // println(s"in CCBT $e")
          recursiveAddTris(getFlip(e), ne)
          e = getNext(e)
          ne = r.getNext(ne)
        } while (e != boundary)

        // Add fans of boundary edges
        do {
          var rot = e
          var rotNe = ne
          do {
            val flip = getFlip(rot)

            val isOuterEdge = outerEdges.contains((getSrc(flip), getVert(flip)))
            if (!isOuterEdge) {
              lookupTriangle(flip) match {
                case Some(tri) =>
                  if(rotNe != r.getFlip(tri)) {
                    join(rotNe, tri)
                    // r.joinTriangles(rotNe, tri)
                  }
                case None =>
                  val tri = copyConvertTriangle(flip)
                  join(rotNe, tri)
                  // r.joinTriangles(rotNe, tri)
                  assert(r.rotCWSrc(rotNe) == r.getNext(tri))
              }
            }
            rot = rotCWSrc(rot)
            rotNe = r.rotCWSrc(rotNe)
            assert(getVert(rot) == r.getVert(rotNe))
            assert(getSrc(rot) == r.getSrc(rotNe))
          } while(rot != e)


          // if(rotNe != ne) {
          //   println(s"NOT THE SAME ${r.getSrc(rotNe)} -> ${r.getVert(rotNe)} and ${r.getSrc(ne)} -> ${r.getVert(ne)}")
          //   println(s"  N: ${r.getNext(rotNe)} F: ${r.getFlip(rotNe)} and N: ${r.getNext(ne)} F: ${r.getFlip(ne)}")
          // }

          e = getNext(e)
          ne = r.getNext(ne)
        } while(e != boundary)

        // TODO - Remove
        // do {
        //   var rot = e
        //   do {
        //     val next = rotCWSrc(rot)
        //     val nextFlip = getFlip(next)

        //     // val isOuterEdge = outerEdges.contains((getSrc(nextFlip), getVert(nextFlip)))
        //     // if (!isOuterEdge) {
        //     if(next != e) {
        //       assert(isCCW(getVert(next), getVert(rot), getSrc(rot)))
        //     }
        //     rot = rotCWSrc(rot)
        //   } while(rot != e)

        //   e = getNext(e)
        //   ne = r.getNext(ne)
        // } while(e != boundary)


        // TODO - Remove
        // do {
        //   var neRot = ne
        //   do {
        //     val next = r.rotCWSrc(neRot)
        //     val nextFlip = r.getFlip(next)

        //     val isOuterEdge = outerEdges.contains((r.getSrc(nextFlip), r.getVert(nextFlip)))
        //     if(next != ne) {
        //       assert(isCCW(r.getVert(next), r.getVert(neRot), r.getSrc(neRot)))
        //     }
        //     neRot = r.rotCWSrc(neRot)
        //   } while(neRot != ne)

        //   e = getNext(e)
        //   ne = r.getNext(ne)
        // } while(e != boundary)

        // println(s"OUT")
        assert(ne == newBound)
        newBound
      }

      if (isLinear)
        copyConvertLinearBound
      else
        copyConvertBoundingTris
    }

    // TODO - Remove
    // for((idx @ (v1, v2, v3), e) <- _triangles) {
    //   if(idx != TriangleMap.regularizeTriangleIndex(r.getVert(e), r.getVert(r.getNext(e)), r.getVert(r.getNext(r.getNext(e))))) {
    //     print(s"$idx !=");  r.showBoundingLoop(e)
    //         val (px1, px2, px3) =
    //           (
    //             points(v1),
    //             points(v2),
    //             points(v3)
    //           )
    //     println(s"KEY ${(v1, v2, v3)}")
    //         println(Polygon(px1.toPoint, px2.toPoint, px3.toPoint, px1.toPoint).toWKT)
    //     println(s"VALUE ${(r.getVert(e), r.getVert(r.getNext(e)), r.getVert(r.getNext(r.getNext(e))))}")
    //         val (ox1, ox2, ox3) =
    //           (
    //             points(r.getVert(e)),
    //             points(r.getVert(r.getNext(e))),
    //             points(r.getVert(r.getNext(r.getNext(e))))
    //           )
    //         println(Polygon(ox1.toPoint, ox2.toPoint, ox3.toPoint, ox1.toPoint).toWKT)
    //         sys.error("Weird different triangles - BMESH")
    //   }
    // }

    BoundingMesh(boundingExtent, r, boundingEdge, _triangles.toMap, isLinear, points)
  }

  type HE = HalfEdge[StitchKey, Unit]

  // TODO: Make this work for meshes that are linear.
  def stitch(neighbors: Map[Direction, BoundingMesh]): HalfEdgeBoundingMesh[StitchKey] = {

    def regularizeTriIndex(idx: (StitchKey, StitchKey, StitchKey)) = {
      idx match {
        case (a, b, c) if (a < b && a < c) => (a, b, c)
        case (a, b, c) if (b < a && b < c) => (b, c, a)
        case (a, b, c) => (c, a, b)
      }
    }

    val halfEdgeMeshes: Map[Direction, HalfEdgeBoundingMesh[StitchKey]] =
      (for((direction, mesh) <- neighbors) yield {
        import mesh.halfEdgeTable._

        val newEdgeMap = mutable.Map[(Int, Int), HalfEdge[StitchKey, Unit]]()

        val _triangles =
          mutable.Map[(StitchKey, StitchKey, StitchKey), HalfEdge[StitchKey, Unit]]()

        def insertTriangle(tri: HalfEdge[StitchKey, Unit]): Unit = {
//          val points = mesh.points
          val idx = regularizeTriIndex((tri.vert, tri.next.vert, tri.next.next.vert))

          // val (p1, p2, p3) =
          //   (
          //     points(tri.vert.idx),
          //     points(tri.next.vert.idx),
          //     points(tri.next.next.vert.idx)
          //   )
          // val poly = Polygon(p1.toPoint, p2.toPoint, p3.toPoint, p1.toPoint)
          // for(((k1, k2, k3), he) <- _triangles) {
          //   val (o1, o2, o3) =
          //     (
          //       points(k1.idx),
          //       points(k2.idx),
          //       points(k3.idx)
          //     )

          //   val poly2 = Polygon(o1.toPoint, o2.toPoint, o3.toPoint, o1.toPoint)

          //   if(poly.intersects(poly2) && !poly.touches(poly2)) {
          //     _triangles += idx -> tri

          //     println(poly.toWKT)
          //     println(poly2.toWKT)
          //     val (px1, px2, px3) =
          //       (
          //         points(idx._1.idx),
          //         points(idx._2.idx),
          //         points(idx._3.idx)
          //       )
          //     println(Polygon(px1.toPoint, px2.toPoint, px3.toPoint, px1.toPoint).toWKT)
          //     val (ox1, ox2, ox3) =
          //       (
          //         points(he.vert.idx),
          //         points(he.next.vert.idx),
          //         points(he.next.next.vert.idx)
          //       )
          //     println(Polygon(ox1.toPoint, ox2.toPoint, ox3.toPoint, ox1.toPoint).toWKT)
          //     val pgs: Seq[Polygon] =
          //       _triangles.keys.map { case (i1, i2, i3) =>
          //         val p1 = points(i1.idx)
          //         val p2 = points(i2.idx)
          //         val p3 = points(i3.idx)

          //         Polygon(Line(p1.toPoint, p2.toPoint, p3.toPoint, p1.toPoint))
          //       }.toSeq

          //     val mp =
          //       MultiPoint(pgs.flatMap(_.vertices)).union.as[MultiPoint].get

          //     val gc =
          //       GeometryCollection(polygons =
          //         _triangles.keys.map { case (i1, i2, i3) =>
          //           val p1 = points(i1.idx)
          //           val p2 = points(i2.idx)
          //           val p3 = points(i3.idx)

          //           val (z1, z2, z3) = (p1.z, p2.z, p3.z)
          //           Polygon(Line(p1.toPoint, p2.toPoint, p3.toPoint, p1.toPoint))
          //         }.toSeq)

          //     val tp = s"/Users/rob/proj/jets/STITCH-DELAUNAY-ERROR.wkt"
          //     write(
          //       tp,
          //       gc.toWKT
          //     )

          //     write(
          //       s"/Users/rob/proj/jets/STITCH-DELAUNAY-ERROR.geojson",
          //       gc.toGeoJson
          //     )

          //     write(
          //       s"/Users/rob/proj/jets/STITCH-DELAUNAY-ERROR-POINTS.geojson",
          //       mp.toGeoJson
          //     )
          //     sys.error(s"DUMPED TREE AT $tp")
          //   }
          //}
          _triangles += idx -> tri
        }

        var boundary: HalfEdge[StitchKey, Unit] = null

        // Walk the boundary w/ flips

        var e = mesh.boundary
        // println("ORIGINAL OG: ")
        // showBoundingLoop(e)

        var first: HalfEdge[StitchKey, Unit] = null
        var last: HalfEdge[StitchKey, Unit] = null
        do {
          boundary = HalfEdge[StitchKey, Unit]((direction, getSrc(e)), (direction, getVert(e)))
          if(first == null) first = boundary
          // println(s"  BOUNDING EDGE ${boundary.src} -> ${boundary.vert}")
          newEdgeMap((getVert(e), getSrc(e))) = boundary.flip
          newEdgeMap((getSrc(e), getVert(e))) = boundary

          if(last != null) {
            boundary.flip.next = last.flip
            last.next = boundary
          }
          last = boundary
          e = getNext(e)
        } while(e != mesh.boundary)

        first.flip.next = last.flip
        last.next = first

        // do {
        //   val nxt = getNext(e)
        //   val boundaryNextFlip = newEdgeMap((getVert(nxt), getSrc(nxt)))
        //   val boundaryFlip = newEdgeMap((getVert(e), getSrc(e)))
        //   boundaryFlip.flip.next =
        //     boundaryFlip

        //   newEdgeMap((getSrc(e), getVert(e))).next =
        //     newEdgeMap((getSrc(nxt), getVert(nxt)))
        //   println(s"   TIED
        //   e = nxt
        // } while(e != mesh.boundary)

        // println("NEW BOUND: ")
//        HalfEdge.showBoundingLoop(boundary)

        // Convert each triangle, checking for inner boundary edges.
        mesh.triangles.foreach { case ((v1, v2, v3), inner) =>
          val e0 = inner
          val e1 = getNext(e0)
          val e2 = getNext(e1)

          val idx: (StitchKey, StitchKey, StitchKey) =
            (
              StitchKey(direction, getVert(e0)),
              StitchKey(direction, getVert(e1)),
              StitchKey(direction, getVert(e2))
            )

          // if(TriangleMap.regularizeTriangleIndex(v1, v2, v3) !=
          //   TriangleMap.regularizeTriangleIndex(getVert(e0), getVert(e1), getVert(e2))) {
          //   val (px1, px2, px3) =
          //     (
          //       mesh.points(v1),
          //       mesh.points(v2),
          //       mesh.points(v3)
          //     )
          //   println(Polygon(px1.toPoint, px2.toPoint, px3.toPoint, px1.toPoint).toWKT)
          //   val (ox1, ox2, ox3) =
          //     (
          //       mesh.points(getVert(e0)),
          //       mesh.points(getVert(e1)),
          //       mesh.points(getVert(e2))
          //     )
          //   println(Polygon(ox1.toPoint, ox2.toPoint, ox3.toPoint, ox1.toPoint).toWKT)
          //   sys.error("Weird different triangles")
          // }

          val tri =
            HalfEdge[StitchKey, Unit](Seq[StitchKey](idx._1, idx._2, idx._3), ()).flip

          insertTriangle(tri)

          var e = tri
          do {
            newEdgeMap.get((e.vert.idx, e.src.idx)) match {
              case None =>
                newEdgeMap += ((e.src.idx, e.vert.idx) -> e)
              case Some(edge) =>
                e.join(edge)
            }
            e = e.next
          } while (e != tri)

          // println("AFTER TRANGLE")
//          HalfEdge.showBoundingLoop(e)

        }

        val points =
          mesh.points.map { case (key, value) => (StitchKey(direction, key), value) }

        // // TODO: Remove
        // _triangles.foreach { case ((v1, v2, v3), inner) =>
        //   val e0 = inner
        //   val e1 = e0.next
        //   val e2 = e1.next

        //   val idx: (StitchKey, StitchKey, StitchKey) =
        //     (
        //       e0.vert,
        //       e1.vert,
        //       e2.vert
        //     )

        //   if(regularizeTriIndex((v1, v2, v3)) !=
        //     regularizeTriIndex(idx)) {
        //     val (px1, px2, px3) =
        //       (
        //         points(v1),
        //         points(v2),
        //         points(v3)
        //       )
        //     println(Polygon(px1.toPoint, px2.toPoint, px3.toPoint, px1.toPoint).toWKT)
        //     val (ox1, ox2, ox3) =
        //       (
        //         points(e0.vert),
        //         points(e1.vert),
        //         points(e2.vert)
        //       )
        //     println(Polygon(ox1.toPoint, ox2.toPoint, ox3.toPoint, ox1.toPoint).toWKT)
        //     sys.error("Weird different triangles")
        //   }
        // }

        (
          direction,
          HalfEdgeBoundingMesh[StitchKey](
            mesh.extent,
            boundary,
            _triangles.toMap,
            mesh.isLinear,
            points
          )
        )
      }).toMap

    // for((k, m) <- halfEdgeMeshes) {
    //   // println(s"$k")
    //   // println("BOUNDARY")
    //   HalfEdge.showBoundingLoop(m.boundary)
    //   // println("TRANGLES")
    //   for((idx, tri) <- m.triangles) {
    //     // println(s"$idx -> ") ; HalfEdge.showBoundingLoop(tri)
    //   }
    //   // println("\n\n")
    // }

//    sys.error("asdf")

    // val gcb = GeometryCollection(polygons =
    //   halfEdgeMeshes.flatMap { case (k, m) =>
    //     m.triangles.keys.map { case (i1, i2, i3) =>
    //       val p1 = Point3D(m.points(i1).x, m.points(i1).y)
    //       val p2 = Point3D(m.points(i2).x, m.points(i2).y)
    //       val p3 = Point3D(m.points(i3).x, m.points(i3).y)

    //       val (z1, z2, z3) = (p1.z, p2.z, p3.z)
    //       Polygon(Line(p1.toPoint, p2.toPoint, p3.toPoint, p1.toPoint))
    //     }.toSeq
    //   }.toSeq)



    // import geotrellis.vector.io._
    // //println(gc.toWKT)
    // write(
    //   s"/Users/rob/proj/jets/STITCHED-INITIAL-HE-BOUNDS.wkt",
    //   gcb.toWKT
    // )


    // Stitch time.
    val points: mutable.Map[StitchKey, Point3D] =
      halfEdgeMeshes.map(_._2.points).reduce(_ ++ _)

    val triangles: mutable.Map[(StitchKey, StitchKey, StitchKey), HalfEdge[StitchKey, Unit]] =
      mutable.Map(halfEdgeMeshes.flatMap(_._2.triangles).toSeq: _*)

    def insertTriangle(idx: (StitchKey, StitchKey, StitchKey), tri: HalfEdge[StitchKey, Unit]): Unit = {
      triangles += regularizeTriIndex(idx) -> tri
    }

    def insertTriangle2(tri: HalfEdge[StitchKey, Unit]): Unit = {
      val idx = (tri.vert, tri.next.vert, tri.next.next.vert)
      insertTriangle(idx, tri)
    }

    def deleteTriangle(idx: (StitchKey, StitchKey, StitchKey)): Unit = {
      triangles -= regularizeTriIndex(idx)
    }

    // TODO: UnDebug
    var deleteCount = 0
    def deleteTriangle2(tri: HalfEdge[StitchKey, Unit]): Unit = {
      val idx = (tri.vert, tri.next.vert, tri.next.next.vert)

      // val p1 = Point3D(points(idx._1).x, points(idx._1).y)
      // val p2 = Point3D(points(idx._2).x, points(idx._2).y)
      // val p3 = Point3D(points(idx._3).x, points(idx._3).y)

      // val p = Polygon(Line(p1.toPoint, p2.toPoint, p3.toPoint, p1.toPoint))
      // val pa = s"/Users/rob/proj/jets/base-stitch/deleted/"
      // if(!new java.io.File(pa).exists) { new java.io.File(pa).mkdir }
      // write(s"/Users/rob/proj/jets/base-stitch/deleted/${baseCount}-${deleteCount}.wkt", p.toWKT)
      // deleteCount += 1
      deleteTriangle(idx)
    }

    def distanceToExtent(lp: Point3D, ex: Extent): Double = {
      val ppd = new PointPairDistance
      DistanceToPoint.computeDistance(ex.toPolygon.jtsGeom, lp.toPoint.jtsGeom.getCoordinate, ppd)
      ppd.getDistance
    }

    def zipper(
      left0: HalfEdge[StitchKey, Unit],
      isLeftLinear: Boolean,
      lExtent: Extent,
      right0: HalfEdge[StitchKey, Unit],
      isRightLinear: Boolean,
      rExtent: Extent
    ): (HalfEdge[StitchKey, Unit], Boolean) = {
      implicit def verts(key: StitchKey): Point3D = {
        points(key)
      }
      import Point3DPredicates._

      def advance(e0: HalfEdge[StitchKey, Unit]): HalfEdge[StitchKey, Unit] = {
        var e = e0.next
        while (!isCorner(e))
          e = e.next
        e
      }
      def reverse(e0: HalfEdge[StitchKey, Unit]): HalfEdge[StitchKey, Unit] = {
        var e = e0.prev
        while (!isCorner(e))
          e = e.prev
        e
      }
      def advanceIfNotCorner(e0: HalfEdge[StitchKey, Unit]) = {
        var e = e0
        while (!isCorner(e))
          e = e.next
        e
      }

      var left = advanceIfNotCorner(left0)
      var right = advanceIfNotCorner(right0)

      if (isLeftLinear) {
        // Pick the endpoint closest to the opposing extent
        val other = advance(left)
        if (distanceToExtent(verts(other.src), rExtent) < distanceToExtent(verts(left.src), rExtent))
          left = other
      }
      if (isRightLinear) {
        val other = advance(right)
        if (distanceToExtent(verts(other.src), lExtent) < distanceToExtent(verts(right.src), lExtent))
          right = other
      }

      // var baseCount = 0
      // def saveBase(b: HalfEdge[StitchKey, Unit], name: String = ""): Unit = {
      //   val p1 = points(b.src)
      //   val p2 = points(b.vert)
      //   val l = Line(p1.toPoint, p2.toPoint)
      //   val p = s"/Users/rob/proj/jets/base-stitch/${baseCount}"
      //   if(!new java.io.File(p).exists) { new java.io.File(p).mkdir }
      //   write(s"/Users/rob/proj/jets/base-stitch/${baseCount}/${name}.wkt", l.toWKT)
      // }

      // compute the lower common tangent of left and right
      var continue = true
      var base: HalfEdge[StitchKey, Unit] = null
      while (continue) {
        base = HalfEdge[StitchKey, Unit](right.src, left.src)
        //             if(isLeftOf(base, getVert(left))) {
        if (isLeftOf(base, left.vert)) {
          // left points to a vertex that is to the left of
          // base, so move base to left.next
          left = advance(left)

            // } else if(!isRightLinear && !isRightOf(base, getVert(right))) {
        } else if(!isRightLinear && !isRightOf(base, right.vert)) {
          // right points to a point that is left of base,
          // so keep walking right
          right = advance(right)

            // } else if(!isLeftLinear && !isRightOf(base, getSrc(getPrev(left)))) {
        } else if (!isLeftLinear && !isRightOf(base, left.prev.src)) {
          // Left's previous source is left of base,
          // so this base would break tangency. Move
          // back to previous left.
          left = reverse(left)
//            } else if(isLeftOf(base, getSrc(getPrev(right)))) {
        } else if (isLeftOf(base, right.prev.src)) {
          // Right's previous source is left of base,
          // so this base would break tangency. Move
          // back to previous right.
          right = reverse(right)
        // } else if(!isRightLinear && isCollinear(base, right.vert)) {
        //   // Right is collinear with base and its dest is closer to left; move right forward.
        //   right = advance(right)
        // } else if (!isLeftLinear && isCollinear(base, left.prev.src)) {
        //   // Left's previous src is collinear with base and is closer to right; move left back.
        //   left = reverse(left)
        } else {
          continue = false
        }
      }

      base.next = left
      base.flip.next = right
      left.prev.next = base.flip
      right.prev.next = base

      // saveBase(base, "initial")
      // baseCount += 1

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
      // var lCanCount = 0
      // var rCanCount = 0
      while (continue) {
        var lcand = base.flip.rotCCWSrc
        var rcand = base.rotCWSrc
        // saveBase(lcand, s"lcand-${lCanCount}")
        // saveBase(rcand, s"rcand-${rCanCount}")
        // lCanCount += 1
        // rCanCount += 1

        // Find left side candidate edge for extending the fill triangulation
        if (isCCW(lcand.vert, base.vert, base.src)) {
          while (inCircle((base.vert, base.src, lcand.vert), lcand.rotCCWSrc.vert)) {
            val e = lcand.rotCCWSrc
            deleteTriangle2(lcand)
            lcand.rotCCWDest.next = lcand.next
            lcand.prev.next = lcand.flip.next
            lcand = e
            // saveBase(lcand, s"lcand-${lCanCount}")
            // lCanCount += 1
          }
        }

        // Find right side candidate edge for extending the fill triangulation
        if (isCCW(rcand.vert, base.vert, base.src)) {
          while (inCircle((base.vert, base.src, rcand.vert), rcand.rotCWSrc.vert)) {
            val e = rcand.rotCWSrc
            deleteTriangle2(rcand.flip)
            base.flip.next = rcand.rotCWSrc
            rcand.rotCCWDest.next = rcand.next
            rcand = e
            // saveBase(rcand, s"rcand-${rCanCount}")
            // rCanCount += 1
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
            val e = HalfEdge[StitchKey, Unit](rcand.vert, base.vert)
            e.flip.next = rcand.next
            e.next = base.flip
            rcand.next = e
            lcand.flip.next = e.flip
            base = e
            // saveBase(base, s"base-lcand-${baseCount}")
            // baseCount += 1
            // lCanCount = 0
            // rCanCount = 0

          } else {
            // form new triangle from lcand and base
            val e = HalfEdge[StitchKey, Unit](base.src, lcand.vert)
            lcand.rotCCWDest.next = e.flip
            e.next = lcand.flip
            e.flip.next = rcand
            base.flip.next = e
            base = e
            // saveBase(base, s"base-lcand-${baseCount}")
            // baseCount += 1
            // lCanCount = 0
            // rCanCount = 0
          }

          // Tag edges with the center of the new triangle's circumscribing circle
          // val c = circleCenter(base.vert, base.next.vert, base.next.next.vert)
          base.face = Some(())
          base.next.face = Some(())
          base.next.next.face = Some(())
          insertTriangle2(base)
        }
      }

      (base.flip.next, false)
    }

    type StitchResult = (Extent, HalfEdge[StitchKey, Unit], Boolean)
    def joinMeshes(in1: StitchResult, in2: StitchResult): StitchResult = {
      val (in1Extent, in1Boundary, in1IsLinear) = in1
      val (in2Extent, in2Boundary, in2IsLinear) = in2
      val (joined, linearResult) =
        zipper(in1Boundary, in1IsLinear, in1Extent, in2Boundary, in2IsLinear, in2Extent)

      val e = in1Extent.expandToInclude(in2Extent)

      // def showTriangles(prefix: String = ""): Unit = {
      //   val gc =
      //     GeometryCollection(polygons =
      //       triangles.keys.map { case (i1, i2, i3) =>
      //         import stitched.points
      //         val p1 = Point3D(points(i1).x, points(i1).y)
      //         val p2 = Point3D(points(i2).x, points(i2).y)
      //         val p3 = Point3D(points(i3).x, points(i3).y)

      //         val (z1, z2, z3) = (p1.z, p2.z, p3.z)
      //         Polygon(Line(p1.toPoint, p2.toPoint, p3.toPoint, p1.toPoint))
      //       }.toSeq)

      //   import geotrellis.vector.io._
      //   //println(gc.toWKT)
      //   write(
      //     s"/Users/rob/proj/jets/stitched-meshes-${in2.extent.xmin}.wkt",
      //     gc.toWKT
      //   )


      (e, joined, linearResult)
    }

    import Direction._
    val rows =
      Seq(
        Seq(TopLeft, Top, TopRight),
        Seq(Left, Center, Right),
        Seq(BottomLeft, Bottom, BottomRight)
      )

    val (e, joined, linearResult) =
      rows
        .map { row =>
          row.flatMap {
            halfEdgeMeshes
              .get(_)
              .map { hem =>
                (hem.extent, hem.boundary, hem.isLinear)
              }
          }
        }
        .filter(!_.isEmpty)
        .map { _.reduce(joinMeshes(_,_)) }
        .reduce(joinMeshes(_, _))

    HalfEdgeBoundingMesh[StitchKey](e, joined, triangles.toMap, linearResult, points)
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
