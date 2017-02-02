package geotrellis.pointcloud.spark.triangulation

import geotrellis.vector.{Extent, MultiPolygon, Point, Polygon}
import geotrellis.vector.triangulation.{DelaunayTriangulation, HalfEdgeTable, Predicates, TriangleMap}

import com.vividsolutions.jts.algorithm.distance.{DistanceToPoint, PointPairDistance}
import com.vividsolutions.jts.geom.Coordinate

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

    // val normalized =
    //   TriangleMap.regularizeIndex(
    //     getDest(tri), getDest(getNext(tri)), getDest(getNext(getNext(tri)))
    //   )
    triangles.get(getDest(tri), getDest(getNext(tri)), getDest(getNext(getNext(tri)))) match {
      case Some(base) => {
        var e = base
        do {
          //println(s"YUP THIS IS IT $e")
          if (navigator.getDest(e) == getDest(tri)) {
            return Some(e)
          }
          e = navigator.getNext(e)
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

  val innerLoop = collection.mutable.ListBuffer.empty[(HalfEdge, ResultEdge)]
  //var innerLoop: (HalfEdge, ResultEdge) = (-1, -1)

  def recursiveAddTris(e0: HalfEdge, opp0: ResultEdge): Unit = {
    import dt.navigator._

    //println("recursiveAddTris")
    val workQueue = collection.mutable.Queue( (e0, opp0) )

    while (!workQueue.isEmpty) {
      val (e, opp) = workQueue.dequeue
      //println(s"     WORKING ON:")
      //showLoop(e)
      //navigator.showLoop(opp)
      //println(s"=======")
      val isOuterEdge = outerEdges.contains((getSrc(e), getDest(e)))
      val isFlipInInnerRing = {
        val flip = navigator.getFlip(opp)
        navigator.getDest(navigator.getNext(navigator.getNext(navigator.getNext(flip)))) != navigator.getDest(flip)
      }
      if (!isOuterEdge && isFlipInInnerRing) {
        //  We are not on the boundary of the original triangulation, and opp.flip isn't already part of a triangle
        lookupTriangle(e) match {
          case Some(tri) =>
            // opp.flip should participate in the existing triangle specified by tri.  Connect opp to tri so that it does.
            //println(s"    --- FOUND TRIANGLE:")
            //navigator.showLoop(tri)
            navigator.join(opp, tri)
            // r.joinTriangles(opp, tri)
            // println(s"    JOIN FOUND TRIANGLE")
            // r.showBoundingLoop(r.getFlip(r.getNext(tri)))
            // r.showBoundingLoop(r.getFlip(r.getNext(opp)))
          case None =>
            // We haven't been here yet, so create a triangle to mirror the one referred to by e, and link opp to it.
            // (If that triangle belongs in the boundary, otherwise, mark opp as part of the inner ring for later)
            //println("     --- DID NOT FIND TRIANGLE")

      //      val tri = copyConvertTriangle(e)
      //      navigator.join(opp, tri)

            if (circumcircleLeavesExtent(boundingExtent)(e)) {
              //println("         Triangle circle leaves extent")
              val tri = copyConvertTriangle(e)

              //print("         ")
              //navigator.showLoop(tri)
              navigator.join(opp, tri)

              workQueue.enqueue( (getFlip(getNext(e)), navigator.getNext(tri)) )
              workQueue.enqueue( (getFlip(getNext(getNext(e))), navigator.getNext(navigator.getNext(tri))) )
            } else {
              innerLoop += ((e, opp))
              //innerLoop = (e, opp)
            }
        }
      }
    }
  }

  def fillInnerLoop(): Unit = {
    import dt.navigator._

    val polys = collection.mutable.ListBuffer.empty[Polygon]
    val bounds = innerLoop.map{ case (_, o) => (navigator.getSrc(o) -> navigator.getDest(o), o) }.toMap
    val boundrefs = innerLoop.map(_._2).toSet

    innerLoop.foreach{ case (e0, o0) =>{
      var e = e0
      var o = navigator.getFlip(o0)

      var continue = true
      var j = 0
      do {
        if (getSrc(e) != navigator.getSrc(o) || getDest(e) != navigator.getDest(o)) {
          new java.io.PrintWriter("buffer.wkt") { write(geotrellis.vector.io.wkt.WKT.write(MultiPolygon(polys))); close } // (debug)

          println(s"Inconsistent state: e = [${getSrc(e)} -> ${getDest(e)}], o = [${navigator.getSrc(o)} -> ${navigator.getDest(o)}]")
          return ()
        }
        
        // pulse (debug)
        println(s"e = ${(getSrc(e), getDest(e), getDest(getNext(e)))}, o = [${navigator.getSrc(o)} -> ${navigator.getDest(o)}]")

        bounds.get(navigator.getSrc(o) -> navigator.getDest(o)) match {
          case Some(oNext) =>
            // we've arrived at the edge.  Make sure we're joined up.
            if (o != oNext)
              navigator.join(navigator.getFlip(o), oNext)
            continue = false

          case None =>
            // not at the boundary.  Keep going.
            
            lookupTriangle(e) match {
              case None =>
                // bounding triangle has not yet been added
                println("ADDING TRIANGLE")
                val tri = copyConvertTriangle(e)

                // add new tri to polys (debugging)
                val a = navigator.getDest(tri)
                val b = navigator.getDest(navigator.getNext(tri))
                val c = navigator.getDest(navigator.getNext(navigator.getNext(tri)))
                val pa = Point(verts(a).x, verts(a).y)
                val pb = Point(verts(b).x, verts(b).y)
                val pc = Point(verts(c).x, verts(c).y)
                polys += Polygon(pa, pb, pc, pa)

                // link new triangle to existing triangulation
                try {
                  navigator.join(tri, navigator.getFlip(o))
                } catch {
                  case _: AssertionError =>
                    println("Improper join (should never see this)")
                    println(geotrellis.vector.io.wkt.WKT.write(Polygon(pa, pb, pc, pa)))
                }

                o = tri
              case Some(tri) =>
                // continue = false

                // bounding triangle already exists
                if (o != tri) {
                  // join if it hasn't already been linked
                  println("join")
                  navigator.join(tri, navigator.getFlip(o))
                  o = tri
                } else {
                  // triangle is already properly connected
                  println("no join") // (debug)
                }
            }
        }

        e = rotCWDest(e)
        o = navigator.rotCWDest(o)
        j += 1
      } while (continue && j < 20)
      if (j == 20)
        println("Premature termination")
    }}

/*
    val e0 = innerLoop._1
    val o0 = innerLoop._2

    var (e, o) = (e0, navigator.getFlip(o0))

    // display the original inner loop
    val vs = collection.mutable.ListBuffer((dt.verts.getX(navigator.getSrc(o)), dt.verts.getY(navigator.getSrc(o))))
    val path = collection.mutable.ListBuffer.empty[(ResultEdge, HalfEdge, ResultEdge)] // (o, e, ONext) 
    var lim = 0
    do {
      val oNext = navigator.getFlip(navigator.getNext(o))

      vs += ((dt.verts.getX(navigator.getDest(o)), dt.verts.getY(navigator.getDest(o))))
      path += ((o, e, oNext))

      var j = 0
      do {
        e = rotCWDest(e)
        j += 1
      } while (getSrc(e) != navigator.getSrc(oNext) && j < 20)
      if (j == 20)
        println("God damn it")

      o = navigator.getFlip(oNext)
      e = getFlip(e)
      lim += 1
    } while (navigator.getDest(navigator.getFlip(o)) != navigator.getDest(o0) && lim < 10000)
    //println(geotrellis.vector.io.wkt.WKT.write(geotrellis.vector.Line(vs)))
    o = navigator.getFlip(o0)

    println(s"Initial condition: e = [${getSrc(e)} -> ${getDest(e)}], o = [${navigator.getSrc(o)} -> ${navigator.getDest(o)}]")
    println(s"Next on inner loop: [${navigator.getSrc(navigator.getNext(o))} -> ${navigator.getDest(navigator.getNext(o))}]")
    println(s"lim = $lim")

    val polys = collection.mutable.ListBuffer.empty[Polygon]
    var i = 0
    path.foreach { case (oInit, eInit, oNext) => {
      o = oInit
      e = eInit
      println(s"oNext = [${navigator.getSrc(oNext)} -> ${navigator.getDest(oNext)}]")
      
      if (getSrc(e) != navigator.getSrc(o) || getDest(e) != navigator.getDest(o)) {
        // send out accumulated triangles (debug)
        new java.io.PrintWriter("buffer.wkt") { write(geotrellis.vector.io.wkt.WKT.write(MultiPolygon(polys))); close }

        println(s"Inconsistent state: e = [${getSrc(e)} -> ${getDest(e)}], o = [${navigator.getSrc(o)} -> ${navigator.getDest(o)}]")
        return ()
      }

      var j = 0
      do {
        // rotate around an innerLoop point, adding and linking triangles
        println(s"e = ${(getSrc(e), getDest(e), getDest(getNext(e)))}, o = [${navigator.getSrc(o)} -> ${navigator.getDest(o)}]")

        lookupTriangle(e) match {
          case None =>
            // bounding triangle has not yet been added
            println("ADDING TRIANGLE")
            val tri = copyConvertTriangle(e)

            // add new tri to polys (debugging)
            val a = navigator.getDest(tri)
            val b = navigator.getDest(navigator.getNext(tri))
            val c = navigator.getDest(navigator.getNext(navigator.getNext(tri)))
            val pa = Point(verts(a).x, verts(a).y)
            val pb = Point(verts(b).x, verts(b).y)
            val pc = Point(verts(c).x, verts(c).y)
            polys += Polygon(pa, pb, pc, pa)

            // link new triangle to existing triangulation
            try {
              navigator.join(tri, navigator.getFlip(o))
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
              navigator.join(tri, navigator.getFlip(o))
              o = tri
            } else {
              // triangle is already properly connected
              println("no join") // (debug)
            }
        }

        e = rotCWDest(e)
        o = navigator.rotCWDest(o)

        // triangle neighborhood should be sane
        if (getSrc(e) != navigator.getSrc(o))
          println(s"Neighborhood out of sync in fillInnerLoop.  Expected ${getSrc(e)}, got ${navigator.getSrc(o)}")
        j += 1
      } while (navigator.getSrc(o) != navigator.getSrc(oNext) && j < 20)

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
      fstO = navigator.rotCCWDest(fstO)
      j += 1
    } while (navigator.getSrc(fstO) != getSrc(fstE) && j < 20)
    if (j == 20)
      println("Problem around first")
    j = 0
    if (lstOnext != navigator.getFlip(fstO))
      println("Didn't complete the loop!")

*/
    new java.io.PrintWriter("buffer.wkt") { write(geotrellis.vector.io.wkt.WKT.write(MultiPolygon(polys))); close }

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

    writeWKT("bounds.wkt")
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

  def writeWKT(wktFile: String) = {
    val indexToCoord = { i: Int => Point.jtsCoord2Point(dt.verts.getCoordinate(i)) }
    val mp = geotrellis.vector.MultiPolygon(triangles.getTriangles.keys.map{ case (i,j,k) => Polygon(indexToCoord(i), indexToCoord(j), indexToCoord(k), indexToCoord(i)) })
    val wktString = geotrellis.vector.io.wkt.WKT.write(mp)
    new java.io.PrintWriter(wktFile) { write(wktString); close }
  }

}
