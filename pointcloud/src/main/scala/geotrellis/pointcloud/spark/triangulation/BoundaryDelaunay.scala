package geotrellis.pointcloud.spark.triangulation

import geotrellis.vector.{Extent, Line, MultiPolygon, Point, Polygon}
import geotrellis.vector.triangulation._

import com.vividsolutions.jts.algorithm.distance.{DistanceToPoint, PointPairDistance}
import com.vividsolutions.jts.geom.Coordinate

object BoundaryDelaunay {
  type HalfEdge = Int
  type ResultEdge = Int
  type Vertex = Int

  def isMeshValid(triangles: TriangleMap, het: HalfEdgeTable): Boolean = {
    import het._
    var valid = true
    triangles.getTriangles.map { case (idx, e0) => {
      val (a, b, c) = idx
      var e = e0
      do {
        if (getFlip(getFlip(e)) != e) {
          println(s"In triangle ${(a,b,c)}: edge ${getSrc(e)} -> ${getDest(e)} has improper flips")
          valid = false
        }
        e = getNext(e)
      } while (e != e0)
    }}
    valid
  }

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

      // val normalized =
      //   TriangleMap.regularizeIndex(
      //     getDest(tri), getDest(getNext(tri)), getDest(getNext(getNext(tri)))
      //   )
      triangles.get(getDest(tri), getDest(getNext(tri)), getDest(getNext(getNext(tri)))) match {
        case Some(base) => {
          var e = base
          do {
            //println(s"YUP THIS IS IT $e")
            if (halfEdgeTable.getDest(e) == getDest(tri)) {
              return Some(e)
            }
            e = halfEdgeTable.getNext(e)
          } while (e != base)

          println("Should never see this")
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

    val outerEdges = collection.mutable.Set.empty[(Vertex, Vertex)]
    val innerEdges = collection.mutable.Map.empty[(Vertex, Vertex), (HalfEdge, ResultEdge)]

    def copyConvertBoundingLoop(): ResultEdge = {
      import dt.halfEdgeTable._

      //println("copyConvertBoundingLoop")
      // Record orig as a outer bound
      // outerEdges += ((getSrc(orig), getDest(orig)))

      // var copy = if (orig == dt.boundary) newBound else copyConvertEdge(orig)

      // if (getNext(orig) != dt.boundary) {
      //   halfEdgeTable.setNext(copy, copyConvertBoundingLoop(getNext(orig), newBound))
      // } else {
      //   halfEdgeTable.setNext(copy, newBound)
      // }
      // halfEdgeTable.setNext(halfEdgeTable.getFlip(halfEdgeTable.getNext(copy)), halfEdgeTable.getFlip(copy))
      // copy

      val first = copyConvertEdge(dt.boundary)
      var last = first
      outerEdges += ((getSrc(dt.boundary), getDest(dt.boundary)))
      innerEdges += (getDest(dt.boundary), getSrc(dt.boundary)) -> (getFlip(dt.boundary), first)
      var e = getNext(dt.boundary)

      do {
        val copy = copyConvertEdge(e)
        outerEdges += ((getSrc(e), getDest(e)))
        innerEdges += (getDest(e), getSrc(e)) -> (getFlip(e), copy)
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

    //val innerLoop = collection.mutable.ListBuffer.empty[(HalfEdge, ResultEdge)]
    //var innerLoop: (HalfEdge, ResultEdge) = (-1, -1)

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
        val isOuterEdge = outerEdges.contains(getSrc(e) -> getDest(e))
        // val isFlipInInnerRing = {
        //   val flip = halfEdgeTable.getFlip(opp)
        //   halfEdgeTable.getDest(halfEdgeTable.getNext(halfEdgeTable.getNext(halfEdgeTable.getNext(flip)))) != halfEdgeTable.getDest(flip)
        // }
        val isInInnerRing = innerEdges.contains(getSrc(e) -> getDest(e))
        if (!isOuterEdge && isInInnerRing) {
          //  We are not on the boundary of the original triangulation, and opp.flip isn't already part of a triangle
          lookupTriangle(e) match {
            case Some(tri) =>
              // opp.flip should participate in the existing triangle specified by tri.  Connect opp to tri so that it does.
              //println(s"    --- FOUND TRIANGLE:")
              //halfEdgeTable.showLoop(tri)
              halfEdgeTable.join(opp, tri)
              innerEdges -= halfEdgeTable.getSrc(tri) -> halfEdgeTable.getDest(tri)
              innerEdges -= halfEdgeTable.getDest(tri) -> halfEdgeTable.getSrc(tri)
              // r.joinTriangles(opp, tri)
              // println(s"    JOIN FOUND TRIANGLE")
              // r.showBoundingLoop(r.getFlip(r.getNext(tri)))
              // r.showBoundingLoop(r.getFlip(r.getNext(opp)))
            case None =>
              // We haven't been here yet, so create a triangle to mirror the one referred to by e, and link opp to it.
              // (If that triangle belongs in the boundary, otherwise, mark opp as part of the inner ring for later)
              //println("     --- DID NOT FIND TRIANGLE")

        //      val tri = copyConvertTriangle(e)
        //      halfEdgeTable.join(opp, tri)

              if (circumcircleLeavesExtent(boundingExtent)(e)) {
                //println("         Triangle circle leaves extent")
                val tri = copyConvertTriangle(e)
                val tri2 = halfEdgeTable.getNext(tri)
                val tri3 = halfEdgeTable.getNext(halfEdgeTable.getNext(tri))

                //print("         ")
                //halfEdgeTable.showLoop(tri)
                halfEdgeTable.join(opp, tri)
                innerEdges -= halfEdgeTable.getSrc(tri) -> halfEdgeTable.getDest(tri)
                innerEdges += (halfEdgeTable.getDest(tri2), halfEdgeTable.getSrc(tri2)) -> (getFlip(getNext(e)), tri2)
                innerEdges += (halfEdgeTable.getDest(tri3), halfEdgeTable.getSrc(tri3)) -> (getFlip(getNext(getNext(e))), tri3)

                workQueue.enqueue( (getFlip(getNext(e)), tri2) )
                workQueue.enqueue( (getFlip(getNext(getNext(e))), tri3) )
              } else {
                //innerLoop += ((e, opp))
                //innerLoop = (e, opp)
              }
          }
        }
      }
    }

    def fillInnerLoop(): Unit = {
      import dt.halfEdgeTable._

      val polys = collection.mutable.ListBuffer.empty[Polygon]
      val bounds = innerEdges.values.map{ case (_, o) => (halfEdgeTable.getSrc(o) -> halfEdgeTable.getDest(o), o) }.toMap
      // val boundrefs = innerEdges.values.map(_._2).toSet

      innerEdges.values.foreach{ case (e0, o0) =>{
        var e = e0
        var pairedE = halfEdgeTable.getFlip(o0)

        // println(s"\u001b[38;5;208m☆ Navigating original mesh ☆\u001b[0m")
        // navigate(e, dt.pointSet.getCoordinate(_), Map.empty)

        var continue = true
        var j = 0
        do {
          // println(s"\u001b[38;5;55m☆ Navigating boundary mesh ☆\u001b[0m")
          // halfEdgeTable.navigate(pairedE, verts(_), Map.empty) 

          assert(getSrc(e) == halfEdgeTable.getSrc(pairedE) && getDest(e) == halfEdgeTable.getDest(pairedE))

          println(s"Building off [${halfEdgeTable.getSrc(pairedE)} -> ${halfEdgeTable.getDest(pairedE)}]")

          // if (getSrc(e) != halfEdgeTable.getSrc(o) || getDest(e) != halfEdgeTable.getDest(o)) {
          //   new java.io.PrintWriter("buffer.wkt") { write(geotrellis.vector.io.wkt.WKT.write(MultiPolygon(polys))); close } // (debug)

          //   println(s"Inconsistent state: e = [${getSrc(e)} -> ${getDest(e)}], o = [${halfEdgeTable.getSrc(o)} -> ${halfEdgeTable.getDest(o)}]")
          //   return ()
          // }

          //println(s"e = ${(getSrc(e), getDest(e), getDest(getNext(e)))}, o = [${halfEdgeTable.getSrc(o)} -> ${halfEdgeTable.getDest(o)}]")

          bounds.get(halfEdgeTable.getSrc(pairedE) -> halfEdgeTable.getDest(pairedE)) match {
            case Some(next) =>
              println("Edge is on bounding loop")

              // we've arrived at the edge.  Make sure we're joined up.
              if (pairedE != next) {
                halfEdgeTable.join(halfEdgeTable.getFlip(pairedE), next)
                pairedE = next
              }
              continue = false

            case None =>
              println("Edge is not on boundary")
              // not at the boundary.  Keep going.

              lookupTriangle(e) match {
                case None =>
                  // bounding triangle has not yet been added
                  //println("ADDING TRIANGLE")
                  val tri = copyConvertTriangle(e)

                  // add new tri to polys (debugging)
                  val a = halfEdgeTable.getDest(tri)
                  val b = halfEdgeTable.getDest(halfEdgeTable.getNext(tri))
                  val c = halfEdgeTable.getDest(halfEdgeTable.getNext(halfEdgeTable.getNext(tri)))
                  val pa = Point(verts(a).x, verts(a).y)
                  val pb = Point(verts(b).x, verts(b).y)
                  val pc = Point(verts(c).x, verts(c).y)
                  polys += Polygon(pa, pb, pc, pa)

                  // link new triangle to existing triangulation
                  try {
                    halfEdgeTable.join(tri, halfEdgeTable.getFlip(pairedE))
                  } catch {
                    case _: AssertionError =>
                      println("Improper join (should never see this)")
                      println(geotrellis.vector.io.wkt.WKT.write(Polygon(pa, pb, pc, pa)))
                  }

                  pairedE = tri
                case Some(tri) =>
                  // continue = false

                  // bounding triangle already exists
                  if (pairedE != tri) {
                    // join if it hasn't already been linked
                    //println("join")
                    halfEdgeTable.join(tri, halfEdgeTable.getFlip(pairedE))
                    pairedE = tri
                  } else {
                    // triangle is already properly connected
                    //println("no join") // (debug)
                  }
              }
          }

          e = rotCWDest(e)
          pairedE = halfEdgeTable.rotCWDest(pairedE)
          j += 1
        } while (continue && j < 20)
        if (j == 20)
          println("Premature termination")
      }}

      // new java.io.PrintWriter("buffer.wkt") { write(geotrellis.vector.io.wkt.WKT.write(MultiPolygon(polys))); close }

  /*
      val e0 = innerLoop._1
      val o0 = innerLoop._2

      var (e, o) = (e0, halfEdgeTable.getFlip(o0))

      // display the original inner loop
      val vs = collection.mutable.ListBuffer((dt.pointSet.getX(halfEdgeTable.getSrc(o)), dt.pointSet.getY(halfEdgeTable.getSrc(o))))
      val path = collection.mutable.ListBuffer.empty[(ResultEdge, HalfEdge, ResultEdge)] // (o, e, ONext)
      var lim = 0
      do {
        val oNext = halfEdgeTable.getFlip(halfEdgeTable.getNext(o))

        vs += ((dt.pointSet.getX(halfEdgeTable.getDest(o)), dt.pointSet.getY(halfEdgeTable.getDest(o))))
        path += ((o, e, oNext))

        var j = 0
        do {
          e = rotCWDest(e)
          j += 1
        } while (getSrc(e) != halfEdgeTable.getSrc(oNext) && j < 20)
        if (j == 20)
          println("God damn it")

        o = halfEdgeTable.getFlip(oNext)
        e = getFlip(e)
        lim += 1
      } while (halfEdgeTable.getDest(halfEdgeTable.getFlip(o)) != halfEdgeTable.getDest(o0) && lim < 10000)
      //println(geotrellis.vector.io.wkt.WKT.write(geotrellis.vector.Line(vs)))
      o = halfEdgeTable.getFlip(o0)

      println(s"Initial condition: e = [${getSrc(e)} -> ${getDest(e)}], o = [${halfEdgeTable.getSrc(o)} -> ${halfEdgeTable.getDest(o)}]")
      println(s"Next on inner loop: [${halfEdgeTable.getSrc(halfEdgeTable.getNext(o))} -> ${halfEdgeTable.getDest(halfEdgeTable.getNext(o))}]")
      println(s"lim = $lim")

      val polys = collection.mutable.ListBuffer.empty[Polygon]
      var i = 0
      path.foreach { case (oInit, eInit, oNext) => {
        o = oInit
        e = eInit
        println(s"oNext = [${halfEdgeTable.getSrc(oNext)} -> ${halfEdgeTable.getDest(oNext)}]")

        if (getSrc(e) != halfEdgeTable.getSrc(o) || getDest(e) != halfEdgeTable.getDest(o)) {
          // send out accumulated triangles (debug)
          new java.io.PrintWriter("buffer.wkt") { write(geotrellis.vector.io.wkt.WKT.write(MultiPolygon(polys))); close }

          println(s"Inconsistent state: e = [${getSrc(e)} -> ${getDest(e)}], o = [${halfEdgeTable.getSrc(o)} -> ${halfEdgeTable.getDest(o)}]")
          return ()
        }

        var j = 0
        do {
          // rotate around an innerLoop point, adding and linking triangles
          println(s"e = ${(getSrc(e), getDest(e), getDest(getNext(e)))}, o = [${halfEdgeTable.getSrc(o)} -> ${halfEdgeTable.getDest(o)}]")

          lookupTriangle(e) match {
            case None =>
              // bounding triangle has not yet been added
              println("ADDING TRIANGLE")
              val tri = copyConvertTriangle(e)

              // add new tri to polys (debugging)
              val a = halfEdgeTable.getDest(tri)
              val b = halfEdgeTable.getDest(halfEdgeTable.getNext(tri))
              val c = halfEdgeTable.getDest(halfEdgeTable.getNext(halfEdgeTable.getNext(tri)))
              val pa = Point(verts(a).x, verts(a).y)
              val pb = Point(verts(b).x, verts(b).y)
              val pc = Point(verts(c).x, verts(c).y)
              polys += Polygon(pa, pb, pc, pa)

              // link new triangle to existing triangulation
              try {
                halfEdgeTable.join(tri, halfEdgeTable.getFlip(o))
              } catch {
                case _: AssertionError =>
                  println("Improper join (should never see this)")
                  println(geotrellis.vector.io.wkt.WKT.write(Polygon(pa, pb, pc, pa)))
              }

              o = tri
            case Some(tri) =>
              // bounding triangle already exists
              if (o != tri) {
                // join if it hasn't already been linked
                println("join")
                halfEdgeTable.join(tri, halfEdgeTable.getFlip(o))
                o = tri
              } else {
                // triangle is already properly connected
                println("no join") // (debug)
              }
          }

          e = rotCWDest(e)
          o = halfEdgeTable.rotCWDest(o)

          // triangle neighborhood should be sane
          if (getSrc(e) != halfEdgeTable.getSrc(o))
            println(s"Neighborhood out of sync in fillInnerLoop.  Expected ${getSrc(e)}, got ${halfEdgeTable.getSrc(o)}")
          j += 1
        } while (halfEdgeTable.getSrc(o) != halfEdgeTable.getSrc(oNext) && j < 20)

        if (j == 20)
          println("Early termination of linking stage")

        println("BUMP")
      }}

      // sanity checks (debug)
      val (_, fstE, fstOnext) = path.head
      val (_, _, lstOnext) = path.last
      var fstO = fstOnext
      var j = 0
      do {
        fstO = halfEdgeTable.rotCCWDest(fstO)
        j += 1
      } while (halfEdgeTable.getSrc(fstO) != getSrc(fstE) && j < 20)
      if (j == 20)
        println("Problem around first")
      j = 0
      if (lstOnext != halfEdgeTable.getFlip(fstO))
        println("Didn't complete the loop!")

  */
    }

    def validate() = {
      import halfEdgeTable._
      val (_, (_, opp0)) = innerEdges.head
      val e0 = getFlip(opp0)
      var e = getNext(e0)

      do {
        if (!innerEdges.contains(getSrc(e) -> getDest(e))) {
          throw new Exception(s"${getSrc(e)} -> ${getDest(e)} not found in innerLoop")
        }

        e = getNext(e)
      } while (e != e0)
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

      isMeshValid(triangles, halfEdgeTable)

      // halfEdgeTable.navigate(
      //   newBound, 
      //   verts(_), 
      //   Map[Char, (String, Int => Int)](
      //     'i' -> (("jump to inner loop", { _ => halfEdgeTable.getFlip(innerEdges.head._2._2) })),
      //     'x' -> (("export loop as WKT", { h => 
      //       val line = Line(halfEdgeTable.mapOverLoop(h){ ix => verts(halfEdgeTable.getDest(ix)) }.map(Point.jtsCoord2Point(_)))
      //       println(geotrellis.vector.io.wkt.WKT.write(line.closed))
      //       h
      //     }))
      //   )
      // )

      // writeWKT("bounds.wkt")
      fillInnerLoop

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

    BoundaryDelaunay(DelaunayPointSet(verts.toMap), halfEdgeTable, triangles, boundary, innerEdges.head._2._2, isLinear)  }

  // def writeWKT(wktFile: String) = {
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
  inner: Int,
  isLinear: Boolean
) {
  def trianglesFromVertices: MultiPolygon = {
    val indexToCoord = { i: Int => Point.jtsCoord2Point(pointSet.getCoordinate(i)) }
    geotrellis.vector.MultiPolygon(
      triangleMap
        .triangleVertices
        .map { case (i, j, k) =>
          Polygon(indexToCoord(i), indexToCoord(j), indexToCoord(k), indexToCoord(i))
        }
    )
  }

  def trianglesFromEdges: MultiPolygon = {
    val indexToCoord = { i: Int => Point.jtsCoord2Point(pointSet.getCoordinate(i)) }
    import halfEdgeTable._
    geotrellis.vector.MultiPolygon(
      triangleMap
        .triangleEdges
        .map { case v =>
          Polygon(indexToCoord(v), indexToCoord(getNext(v)), indexToCoord(getNext(getNext(v))), indexToCoord(v))
        }
    )
  }
  def writeWkt(wktFile: String) = {
    val indexToCoord = { i: Int => Point.jtsCoord2Point(pointSet.getCoordinate(i)) }
    val mp = geotrellis.vector.MultiPolygon(triangleMap.triangleVertices.map{ case (i,j,k) => Polygon(indexToCoord(i), indexToCoord(j), indexToCoord(k), indexToCoord(i)) })
    val wktString = geotrellis.vector.io.wkt.WKT.write(mp)
    new java.io.PrintWriter(wktFile) { write(wktString); close }
  }

  def isMeshValid(): Boolean = { BoundaryDelaunay.isMeshValid(triangleMap, halfEdgeTable) }

  def navigate() = halfEdgeTable.navigate(boundary, 
                                          pointSet.getCoordinate(_), 
                                          Map[Char, (String, Int => Int)](
                                            'i' -> (("jump to inner edge", { _ => inner }))
                                          )
                                         )
}
