package geotrellis.spark.triangulation

import geotrellis.vector.{Extent, Line, MultiPolygon, Point, Polygon, RobustPredicates}
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

    def writeWKT(wktFile: String) = {
      val indexToCoord = { i: Int => Point.jtsCoord2Point(dt.pointSet.getCoordinate(i)) }
      val mp = geotrellis.vector.MultiPolygon(triangles.getTriangles.keys.map{ case (i,j,k) => Polygon(indexToCoord(i), indexToCoord(j), indexToCoord(k), indexToCoord(i)) })
      val wktString = geotrellis.vector.io.wkt.WKT.write(mp)
      new java.io.PrintWriter(wktFile) { write(wktString); close }
    }

    def navigateThis(e0: Int) = {
      halfEdgeTable.navigate(e0, verts.apply(_), Map[Char, (String, Int => Int)](
        'x' -> (("export current loop to loop.wkt", { einit =>
          import halfEdgeTable._
          var e = einit
          val pts = collection.mutable.ListBuffer[Coordinate](verts(getSrc(e)))
          do {
            pts += verts(getDest(e))
            e = getNext(e)
          } while (e != einit)
          val loop = Polygon(pts.map(Point.jtsCoord2Point(_)))
          new java.io.PrintWriter("loop.wkt") { write(geotrellis.vector.io.wkt.WKT.write(loop)); close }
          einit
        })),
        'c' -> (("print the center of the current triangle's circumscribing circle", { e =>
          val predicates = new TriangulationPredicates(IndexedPointSet(verts.toMap), halfEdgeTable)
          import predicates._
          import halfEdgeTable._
          val (_, center, valid) = circleCenter(getDest(e), getDest(getNext(e)), getDest(getNext(getNext(e))))
          println(s"Circle center: $center " + {if (valid) "[valid]" else "[invalid]"})
          e
        })),
        't' -> (("export triangles to triangles.wkt", { e => 
          val polys = collection.mutable.ListBuffer.empty[Polygon]
          val trans = { i : Int => Point.jtsCoord2Point(verts(i)) }
          triangles.triangleVertices.foreach { case (i, j, k) => {
            polys += Polygon(trans(i), trans(j), trans(k), trans(i))
          }}
          val mp = MultiPolygon(polys)
          new java.io.PrintWriter("triangles.wkt"){ write(geotrellis.vector.io.wkt.WKT.write(mp)); close }
          e
        }))
      ))
    }

    def circumcircleLeavesExtent(extent: Extent)(tri: HalfEdge): Boolean = {
      import dt.halfEdgeTable._
      import dt.predicates._

      val (radius, center, valid) = circleCenter(getDest(tri), getDest(getNext(tri)), getDest(getNext(getNext(tri))))

      !valid || {
        val ppd = new PointPairDistance
        DistanceToPoint.computeDistance(extent.toPolygon.jtsGeom, center, ppd)
        ppd.getDistance < radius
      }
    }

    def inclusionTest(extent: Extent, thresh: Double)(tri: HalfEdge): Boolean = {
      import dt.halfEdgeTable._

      val (i, j, k) = (getSrc(tri), getDest(tri), getDest(getNext(tri)))
      val trans = dt.pointSet.getCoordinate(_)

      val pi = trans(i)
      val pj = trans(j)
      val pk = trans(k)

      val (radius, center, _) = dt.predicates.circleCenter(i, j, k)
      val shortest = Seq(pi.distance(pj), pi.distance(pk), pj.distance(pk)).min
      
      if (radius / shortest > thresh)
        true
      else {
        val Extent(x0, y0, x1, y1) = extent
        def outside(x: Double): Boolean = x < 0.0 || x > 1.0

        x1 - radius < x0 + radius || 
        y1 - radius < y0 + radius || 
        outside((center.x - (x0 + radius)) / ((x1 - radius) - (x0 + radius))) || 
        outside((center.y - (y0 + radius)) / ((y1 - radius) - (y0 + radius)))
      }
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

      triangles.get(getDest(tri), getDest(getNext(tri)), getDest(getNext(getNext(tri)))) match {
        case Some(base) => {
          var e = base
          do {
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

      val a = addPoint(getDest(tri))
      val b = addPoint(getDest(getNext(tri)))
      val c = addPoint(getDest(getNext(getNext(tri))))

      val copy =
        halfEdgeTable.getFlip(
          halfEdgeTable.getNext(
            halfEdgeTable.createHalfEdges(a, b, c)
          )
        )

      triangles += (a, b, c) -> copy
      copy
    }

    def recursiveAddTris(e0: HalfEdge, opp0: ResultEdge): Unit = {
      import dt.halfEdgeTable._

      val workQueue = collection.mutable.Queue( (e0, opp0) )

      while (!workQueue.isEmpty) {
        val (e, opp) = workQueue.dequeue
        val isOuterEdge = outerEdges.contains(getSrc(e) -> getDest(e))
        val isInInnerRing = innerEdges.contains(getSrc(e) -> getDest(e))
        if (!isOuterEdge && isInInnerRing) {
          //  We are not on the boundary of the original triangulation, and opp.flip isn't already part of a triangle
          lookupTriangle(e) match {
            case Some(tri) =>
              // opp.flip should participate in the existing triangle specified by tri.  Connect opp to tri so that it does.
              halfEdgeTable.join(opp, tri)
              innerEdges -= halfEdgeTable.getSrc(tri) -> halfEdgeTable.getDest(tri)
              innerEdges -= halfEdgeTable.getDest(tri) -> halfEdgeTable.getSrc(tri)
            case None =>
              // We haven't been here yet, so create a triangle to mirror the one referred to by e, and link opp to it.
              // (If that triangle belongs in the boundary, otherwise, mark opp as part of the inner ring for later)

              if (inclusionTest(boundingExtent, 5)(e)) {
                val tri = copyConvertTriangle(e)
                val tri2 = halfEdgeTable.getNext(tri)
                val tri3 = halfEdgeTable.getNext(halfEdgeTable.getNext(tri))

                halfEdgeTable.join(opp, tri)
                innerEdges -= halfEdgeTable.getSrc(tri) -> halfEdgeTable.getDest(tri)
                innerEdges += (halfEdgeTable.getDest(tri2), halfEdgeTable.getSrc(tri2)) -> (getFlip(getNext(e)), tri2)
                innerEdges += (halfEdgeTable.getDest(tri3), halfEdgeTable.getSrc(tri3)) -> (getFlip(getNext(getNext(e))), tri3)

                workQueue.enqueue( (getFlip(getNext(e)), tri2) )
                workQueue.enqueue( (getFlip(getNext(getNext(e))), tri3) )
              }
          }
        }
      }
    }

    def fillInnerLoop(): Unit = {
      import dt.halfEdgeTable._

      val polys = collection.mutable.ListBuffer.empty[Polygon]
      val bounds = innerEdges.values.map{ case (_, o) => (halfEdgeTable.getSrc(o) -> halfEdgeTable.getDest(o), o) }.toMap

      innerEdges.values.foreach{ case (e0, o0) =>{
        var e = e0
        var pairedE = halfEdgeTable.getFlip(o0)

        var continue = true
        var j = 0
        do {
          bounds.get(halfEdgeTable.getSrc(pairedE) -> halfEdgeTable.getDest(pairedE)) match {
            case Some(next) =>
              // we've arrived at the edge.  Make sure we're joined up.
              if (pairedE != next) {
                halfEdgeTable.join(halfEdgeTable.getFlip(pairedE), next)
                pairedE = next
              }
              continue = false

            case None =>
              // not at the boundary.  Keep going.

              lookupTriangle(e) match {
                case None =>
                  // bounding triangle has not yet been added
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
                  // bounding triangle already exists
                  if (pairedE != tri) {
                    // join if it hasn't already been linked
                    halfEdgeTable.join(tri, halfEdgeTable.getFlip(pairedE))
                    pairedE = tri
                  }
              }
          }

          e = rotCWDest(e)
          pairedE = halfEdgeTable.rotCWDest(pairedE)
          j += 1
        } while (continue)
      }}

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

      val newBound: ResultEdge = copyConvertBoundingLoop()
      var e = dt.boundary
      var ne = newBound

      val boundingTris = collection.mutable.Set.empty[Int]
      do {
        assert(getDest(e) == halfEdgeTable.getDest(ne) && getSrc(e) == halfEdgeTable.getSrc(ne))
        recursiveAddTris(getFlip(e), ne)
        e = getNext(e)
        ne = halfEdgeTable.getNext(ne)
      } while (e != dt.boundary)

      fillInnerLoop

      newBound
    }

    val boundary =
      if (dt.isLinear)
        copyConvertLinearBound
      else
        copyConvertBoundingTris

    BoundaryDelaunay(IndexedPointSet(verts.toMap), halfEdgeTable, triangles, boundary, /*innerEdges.head._2._2,*/ isLinear)  }

}

case class BoundaryDelaunay(
  pointSet: IndexedPointSet,
  halfEdgeTable: HalfEdgeTable,
  triangleMap: TriangleMap,
  boundary: Int,
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
}
