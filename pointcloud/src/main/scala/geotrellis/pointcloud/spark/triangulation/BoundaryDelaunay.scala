package geotrellis.pointcloud.spark.triangulation

import geotrellis.vector.{Extent, MultiPolygon, Point, Polygon}
import geotrellis.vector.triangulation._

import com.vividsolutions.jts.algorithm.distance.{DistanceToPoint, PointPairDistance}
import com.vividsolutions.jts.geom.Coordinate

object BoundaryDelaunay {
  type HalfEdge = Int
  type ResultEdge = Int
  type Vertex = Int

  def apply(dt: DelaunayTriangulation, boundingExtent: Extent): BoundaryDelaunay = {

    val verts = collection.mutable.Map[Vertex, Coordinate]()
    val halfEdgeTable = new HalfEdgeTable(3 * dt.pointSet.length - 6)  // Allocate for half as many edges as would be expected

    val isLinear = dt.isLinear

    def addPoint(v: Vertex): Vertex = {
      val ix = v
      verts.getOrElseUpdate(ix, new Coordinate(dt.pointSet.getX(v), dt.pointSet.getY(v), dt.pointSet.getZ(v)))
      ix
    }

    def addHalfEdges(a: Vertex, b: Vertex): ResultEdge = {
      halfEdgeTable.createHalfEdges(a, b)
    }

    val triangles = new TriangleMap(halfEdgeTable)

    def circumcircleLeavesExtent(extent: Extent)(tri: HalfEdge): Boolean = {
      import dt.halfEdgeTable._
      import dt.predicates._
      implicit val trans = dt.pointSet.getCoordinate(_)

      val center = circleCenter(getDest(tri), getDest(getNext(tri)), getDest(getNext(getNext(tri))))
      val radius = center.distance(new Coordinate(dt.pointSet.getX(getDest(tri)), dt.pointSet.getY(getDest(tri))))
      val ppd = new PointPairDistance

      DistanceToPoint.computeDistance(extent.toPolygon.jtsGeom, center, ppd)
      ppd.getDistance < radius
    }

    /*
     * A function to convert an original triangle (referencing HalfEdge and Vertex)
     * to a local triangle (referencing ResultEdge and Vertex).  The return value
     * is either None if the corresponding triangle has not been added to
     * the local mesh, or Some(edge) where edge points to the same corresponding
     * vertices in the local mesh (i.e., getDest(edge) == getDest(orig)).
     */
    def lookupTriangle(tri: HalfEdge): Option[ResultEdge] = {
      import dt.halfEdgeTable._

      val normalized =
        TriangleMap.regularizeIndex(
          getDest(tri), getDest(getNext(tri)), getDest(getNext(getNext(tri)))
        )
      triangles.get(normalized) match {
        case Some(base) => {
          var e = base
          do {
            //println(s"YUP THIS IS IT $e")
            if (halfEdgeTable.getDest(e) == getDest(tri)) {
              return Some(e)
            }
            e = halfEdgeTable.getNext(e)
          } while (e != base)

          Some(base)
        }
        case None => {
          None
        }
      }
    }

    def copyConvertEdge(e: HalfEdge): ResultEdge = {
      import dt.halfEdgeTable._

      addPoint(getSrc(e))
      addPoint(getDest(e))
      addHalfEdges(getSrc(e), getDest(e))
    }

    def copyConvertLinearBound(): ResultEdge = {
      import dt.halfEdgeTable._

      //println("copyConvertLinearBound")
      val correspondingEdge = collection.mutable.Map.empty[(Vertex, Vertex), ResultEdge]
      var e = dt.boundary

      do {
        val edge = halfEdgeTable.createHalfEdge(getDest(e))
        addPoint(getDest(e))
        correspondingEdge += (getSrc(e), getDest(e)) -> edge
        e = getNext(e)
      } while (e != dt.boundary)

      do {
        val edge = correspondingEdge((getSrc(e), getDest(e)))
        val flip = correspondingEdge((getDest(e), getSrc(e)))
        halfEdgeTable.setFlip(edge, flip)
        halfEdgeTable.setFlip(flip, edge)
        halfEdgeTable.setNext(edge, correspondingEdge((getDest(e), getDest(getNext(e)))))
        e = getNext(e)
      } while (e != dt.boundary)

      correspondingEdge((getSrc(dt.boundary), getDest(dt.boundary)))
    }

    val outerEdges = collection.mutable.Set[(Vertex, Vertex)]()

    def copyConvertBoundingLoop(): ResultEdge = {
      import dt.halfEdgeTable._

      //println("copyConvertBoundingLoop")
      // Record orig as a outer bound
      // outerEdges += ((getSrc(orig), getDest(orig)))

      // var copy = if (orig == dt.boundary) newBound else copyConvertEdge(orig)

      // if (getNext(orig) != dt.boundary) {
      //   navigator.setNext(copy, copyConvertBoundingLoop(getNext(orig), newBound))
      // } else {
      //   navigator.setNext(copy, newBound)
      // }
      // navigator.setNext(navigator.getFlip(navigator.getNext(copy)), navigator.getFlip(copy))
      // copy

      outerEdges += ((getSrc(dt.boundary), getDest(dt.boundary)))
      val first = copyConvertEdge(dt.boundary)
      var last = first
      var e = getNext(dt.boundary)

      do {
        outerEdges += ((getSrc(e), getDest(e)))
        val copy = copyConvertEdge(e)
        halfEdgeTable.setNext(last, copy)
        halfEdgeTable.setNext(halfEdgeTable.getFlip(copy), halfEdgeTable.getFlip(last))
        last = copy
        e = getNext(e)
      } while (e != dt.boundary)
      halfEdgeTable.setNext(last, first)
      halfEdgeTable.setNext(halfEdgeTable.getFlip(first), halfEdgeTable.getFlip(last))

      first
    }

    def copyConvertTriangle(tri: HalfEdge): ResultEdge = {
      import dt.halfEdgeTable._

      //println(s"COPY CONV BEG")
      val a = addPoint(getDest(tri))
      val b = addPoint(getDest(getNext(tri)))
      val c = addPoint(getDest(getNext(getNext(tri))))

      val copy =
        halfEdgeTable.getFlip(
          halfEdgeTable.getNext(
            // halfEdgeTable.createHalfEdges(getDest(tri),
            //                           getDest(getNext(tri)),
            //                           getDest(getNext(getNext(tri))))
            halfEdgeTable.createHalfEdges(a, b, c)
          )
        )

      // val idx =
      //   TriangleMap.regularizeTriangleIndex(
      //     halfEdgeTable.getDest(copy),
      //     halfEdgeTable.getDest(halfEdgeTable.getNext(copy)),
      //     halfEdgeTable.getDest(halfEdgeTable.getNext(halfEdgeTable.getNext(copy)))
      //   )

      triangles += (a, b, c) -> copy
      // println(s"COPY CONV END")
      // print(s" ADDIN TRIANGLE ${idx}: ")
      // showBoundingLoop(tri)
      // println("      ADD +++++++++")
      // r.showBoundingLoop(copy)
      copy
    }

    def recursiveAddTris(e0: HalfEdge, opp0: ResultEdge): Unit = {
      import dt.halfEdgeTable._

      //println("recursiveAddTris")
      val workQueue = collection.mutable.Queue( (e0, opp0) )

      while (!workQueue.isEmpty) {
        val (e, opp) = workQueue.dequeue
        //println(s"     WORKING ON:")
        //showLoop(e)
        //halfEdgeTable.showLoop(opp)
        //println(s"=======")
        val isOuterEdge = outerEdges.contains((getSrc(e), getDest(e)))
        val isFlipOuterEdge = {
          val flip = halfEdgeTable.getFlip(opp)
          halfEdgeTable.getDest(halfEdgeTable.getNext(halfEdgeTable.getNext(halfEdgeTable.getNext(flip)))) != halfEdgeTable.getDest(flip)
        }
        if (!isOuterEdge && isFlipOuterEdge) {
          //  opp.flip must be boundary edge (isn't interior to triangle)
          lookupTriangle(e) match {
            case Some(tri) =>
              //println(s"    --- FOUND TRIANGLE:")
              //halfEdgeTable.showLoop(tri)
              halfEdgeTable.join(opp, tri)
            // r.joinTriangles(opp, tri)
            // println(s"    JOIN FOUND TRIANGLE")
            // r.showBoundingLoop(r.getFlip(r.getNext(tri)))
            // r.showBoundingLoop(r.getFlip(r.getNext(opp)))
            case None =>
              //println("     --- DID NOT FIND TRIANGLE")
              val tri = copyConvertTriangle(e)
              halfEdgeTable.join(opp, tri)

              if (circumcircleLeavesExtent(boundingExtent)(e)) {
                //println("         Triangle circle leaves extent")
        //        val tri = copyConvertTriangle(e)

                //print("         ")
                //halfEdgeTable.showLoop(tri)
        //        halfEdgeTable.join(opp, tri)

                workQueue.enqueue( (getFlip(getNext(e)), halfEdgeTable.getNext(tri)) )
                workQueue.enqueue( (getFlip(getNext(getNext(e))), halfEdgeTable.getNext(halfEdgeTable.getNext(tri))) )
              }
          }
        }
      }
    }

    def copyConvertBoundingTris(): ResultEdge = {
      import dt.halfEdgeTable._

      //println("copyConvertBoundingTris")
      // val newBound: ResultEdge = copyConvertBoundingLoop(boundary, copyConvertEdge(boundary))
      val newBound: ResultEdge = copyConvertBoundingLoop()
      var e = dt.boundary
      var ne = newBound
      val boundingTris = collection.mutable.Set.empty[Int]
      do {
        // println(s"in CCBT $e")
        recursiveAddTris(getFlip(e), ne)
        e = getNext(e)
        ne = halfEdgeTable.getNext(ne)
      } while (e != dt.boundary)

      // // Add fans of boundary edges
      // do {
      //   var rot = e
      //   var rotNe = ne
      //   do {
      //     val flip = getFlip(rot)

      //     val isOuterEdge = outerEdges.contains((getSrc(flip), getDest(flip)))
      //     if (!isOuterEdge) {
      //       lookupTriangle(flip) match {
      //         case Some(tri) =>
      //           if(rotNe != halfEdgeTable.getFlip(tri)) {
      //             halfEdgeTable.join(rotNe, tri)
      //             // halfEdgeTable.joinTriangles(rotNe, tri)
      //           }
      //         case None =>
      //           val tri = copyConvertTriangle(flip)
      //           halfEdgeTable.join(rotNe, tri)
      //           // halfEdgeTable.joinTriangles(rotNe, tri)
      //           assert(halfEdgeTable.rotCWSrc(rotNe) == halfEdgeTable.getNext(tri))
      //       }
      //     }
      //     rot = rotCWSrc(rot)
      //     rotNe = halfEdgeTable.rotCWSrc(rotNe)
      //     assert(getDest(rot) == halfEdgeTable.getDest(rotNe))
      //     assert(getSrc(rot) == halfEdgeTable.getSrc(rotNe))
      //   } while(rot != e)

      //   e = getNext(e)
      //   ne = halfEdgeTable.getNext(ne)
      // } while(e != dt.boundary)

      assert(ne == newBound)
      newBound
    }

    val boundary =
      if (dt.isLinear)
        copyConvertLinearBound
      else
        copyConvertBoundingTris

    BoundaryDelaunay(DelaunayPointSet(verts.toMap), halfEdgeTable, triangles, boundary, isLinear)
  }

  // // TODO: Remove
  // def writeWKT(dt: DelaunayTriangulation, wktFile: String) = {
  //   val indexToCoord = { i: Int => Point.jtsCoord2Point(dt.pointSet.getCoordinate(i)) }
  //   val mp = geotrellis.vector.MultiPolygon(triangles.getTriangles.keys.map{ case (i,j,k) => Polygon(indexToCoord(i), indexToCoord(j), indexToCoord(k), indexToCoord(i)) })
  //   val wktString = geotrellis.vector.io.wkt.WKT.write(mp)
  //   new java.io.PrintWriter(wktFile) { write(wktString); close }
  // }

}

case class BoundaryDelaunay(
  pointSet: DelaunayPointSet,
  halfEdgeTable: HalfEdgeTable,
  triangleMap: TriangleMap,
  boundary: Int,
  isLinear: Boolean
)
