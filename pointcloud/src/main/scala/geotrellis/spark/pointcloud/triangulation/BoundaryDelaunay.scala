package geotrellis.spark.pointcloud.triangulation

import com.vividsolutions.jts.algorithm.distance.{DistanceToPoint, PointPairDistance}
import com.vividsolutions.jts.geom.Coordinate

import geotrellis.spark.buffer.Direction
import geotrellis.spark.buffer.Direction._
import geotrellis.vector.Extent
import geotrellis.vector.triangulation.{DelaunayTriangulation, HalfEdgeTable, Predicates, TriangleMap}

case class BoundaryDelaunay (dt: DelaunayTriangulation, boundingExtent: Extent) {
  type HalfEdge = Int
  type ResultEdge = Int
  type Vertex = Int

  val verts = collection.mutable.Map[Vertex, Coordinate]()
  val navigator = new HalfEdgeTable(3 * dt.verts.length - 6)  // Allocate for half as many edges as would be expected

  val isLinear = dt.isLinear

  def addPoint(v: Vertex): Vertex = {
    val ix = v
    verts.getOrElseUpdate(ix, new Coordinate(dt.verts.getX(v), dt.verts.getY(v), dt.verts.getZ(v)))
    ix
  }

  def addHalfEdges(a: Vertex, b: Vertex): ResultEdge = {
    navigator.createHalfEdges(a, b)
  }

  val triangles = new TriangleMap

  def circumcircleLeavesExtent(extent: Extent)(tri: HalfEdge): Boolean = {
    import dt.navigator._
    implicit val trans = dt.verts.getCoordinate(_)

    val center = Predicates.circleCenter(getDest(tri), getDest(getNext(tri)), getDest(getNext(getNext(tri))))
    val radius = center.distance(new Coordinate(dt.verts.getX(getDest(tri)), dt.verts.getY(getDest(tri))))
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
    import dt.navigator._

    val normalized =
      TriangleMap.regularizeIndex(
        getDest(tri), getDest(getNext(tri)), getDest(getNext(getNext(tri)))
      )
    triangles.get(normalized) match {
      case Some(base) => {
        var e = base
        do {
          //println(s"YUP THIS IS IT $e")
          if (navigator.getDest(e) == getDest(tri)) {
            return Some(e)
          }
          e = navigator.getNext(e)
        } while (e != base)

        Some(base)
      }
      case None => {
        None
      }
    }
  }

  def copyConvertEdge(e: HalfEdge): ResultEdge = {
    import dt.navigator._

    addPoint(getSrc(e))
    addPoint(getDest(e))
    addHalfEdges(getSrc(e), getDest(e))
  }

  def copyConvertLinearBound(): ResultEdge = {
    import dt.navigator._

    //println("copyConvertLinearBound")
    val correspondingEdge = collection.mutable.Map.empty[(Vertex, Vertex), ResultEdge]
    var e = dt.boundary

    do {
      val edge = navigator.createHalfEdge(getDest(e))
      addPoint(getDest(e))
      correspondingEdge += (getSrc(e), getDest(e)) -> edge
      e = getNext(e)
    } while (e != dt.boundary)

    do {
      val edge = correspondingEdge((getSrc(e), getDest(e)))
      val flip = correspondingEdge((getDest(e), getSrc(e)))
      navigator.setFlip(edge, flip)
      navigator.setFlip(flip, edge)
      navigator.setNext(edge, correspondingEdge((getDest(e), getDest(getNext(e)))))
      e = getNext(e)
    } while (e != dt.boundary)

    correspondingEdge((getSrc(dt.boundary), getDest(dt.boundary)))
  }

  val outerEdges = collection.mutable.Set[(Vertex, Vertex)]()

  def copyConvertBoundingLoop(): ResultEdge = {
    import dt.navigator._

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
      navigator.setNext(last, copy)
      navigator.setNext(navigator.getFlip(copy), navigator.getFlip(last))
      last = copy
      e = getNext(e)
    } while (e != dt.boundary)
    navigator.setNext(last, first)
    navigator.setNext(navigator.getFlip(first), navigator.getFlip(last))

    first
  }

  def copyConvertTriangle(tri: HalfEdge): ResultEdge = {
    import dt.navigator._

    //println(s"COPY CONV BEG")
    val a = addPoint(getDest(tri))
    val b = addPoint(getDest(getNext(tri)))
    val c = addPoint(getDest(getNext(getNext(tri))))

    val copy =
      navigator.getFlip(
        navigator.getNext(
          // navigator.createHalfEdges(getDest(tri),
          //                           getDest(getNext(tri)),
          //                           getDest(getNext(getNext(tri))))
          navigator.createHalfEdges(a, b, c)
        )
      )

    // val idx =
    //   TriangleMap.regularizeTriangleIndex(
    //     navigator.getDest(copy),
    //     navigator.getDest(navigator.getNext(copy)),
    //     navigator.getDest(navigator.getNext(navigator.getNext(copy)))
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
    import dt.navigator._

    println("recursiveAddTris")
    val workQueue = collection.mutable.Queue( (e0, opp0) )

    while (!workQueue.isEmpty) {
      val (e, opp) = workQueue.dequeue
      println(s"     WORKING ON:")
      showLoop(e)
      navigator.showLoop(opp)
      println(s"=======")
      val isOuterEdge = outerEdges.contains((getSrc(e), getDest(e)))
      val isFlipOuterEdge = {
        val flip = navigator.getFlip(opp)
        navigator.getDest(navigator.getNext(navigator.getNext(navigator.getNext(flip)))) != navigator.getDest(flip)
      }
      if (!isOuterEdge && isFlipOuterEdge) {
        //  opp.flip must be boundary edge (isn't interior to triangle)
        lookupTriangle(e) match {
          case Some(tri) =>
            println(s"    --- FOUND TRIANGLE:")
            navigator.showLoop(tri)
            navigator.join(opp, tri)
            // r.joinTriangles(opp, tri)
            // println(s"    JOIN FOUND TRIANGLE")
            // r.showBoundingLoop(r.getFlip(r.getNext(tri)))
            // r.showBoundingLoop(r.getFlip(r.getNext(opp)))
          case None =>
            println("     --- DID NOT FIND TRIANGLE")
            if (circumcircleLeavesExtent(boundingExtent)(e)) {
              println("         Triangle circle leaves extent")
              val tri = copyConvertTriangle(e)
              print("         ")
              navigator.showLoop(tri)
              navigator.join(opp, tri)

              workQueue.enqueue( (getFlip(getNext(e)), navigator.getNext(tri)) )
              workQueue.enqueue( (getFlip(getNext(getNext(e))), navigator.getNext(navigator.getNext(tri))) )
            }
        }
      }
    }
  }

  def copyConvertBoundingTris(): ResultEdge = {
    import dt.navigator._

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
      ne = navigator.getNext(ne)
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
    //           if(rotNe != navigator.getFlip(tri)) {
    //             navigator.join(rotNe, tri)
    //             // navigator.joinTriangles(rotNe, tri)
    //           }
    //         case None =>
    //           val tri = copyConvertTriangle(flip)
    //           navigator.join(rotNe, tri)
    //           // navigator.joinTriangles(rotNe, tri)
    //           assert(navigator.rotCWSrc(rotNe) == navigator.getNext(tri))
    //       }
    //     }
    //     rot = rotCWSrc(rot)
    //     rotNe = navigator.rotCWSrc(rotNe)
    //     assert(getDest(rot) == navigator.getDest(rotNe))
    //     assert(getSrc(rot) == navigator.getSrc(rotNe))
    //   } while(rot != e)

    //   e = getNext(e)
    //   ne = navigator.getNext(ne)
    // } while(e != dt.boundary)

    assert(ne == newBound)
    newBound
  }

  val boundary =
    if (dt.isLinear)
      copyConvertLinearBound
    else
      copyConvertBoundingTris

}
