package geotrellis.vector.triangulation

import org.apache.commons.math3.linear.{MatrixUtils, RealMatrix}
import spire.syntax.cfor._

import geotrellis.vector._
import geotrellis.vector.io.wkt.WKT

import scala.annotation.tailrec
import scala.collection.mutable.{ListBuffer, Map, PriorityQueue, Set}

/** Generates the Delaunay triangulation of a set of points.
 *
 * The DelaunayTriangulation class operates on a collection of Coordinates to
 * produce the unique triangulation (after projecting the points to the x-y
 * plane) that satisfies the condition of each triangle in the result having a
 * circumscribing circle where no vertices of the input point set are in the
 * interior of that circle.
 *
 * Note that the input set must have 2 or more distinct points.  Collinear points
 * are allowed.
 *
 * Whenever using this class for complex tasks with large quantities of points,
 * numerical issues must be considered.  First, this triangulator will pare down
 * the input set so as to remove points which are not numerically distinct.  This
 * may result in points which are not strictly equal, but indistinguishable from
 * one another with respect to the geometric predicates (collinearity tests,
 * in-circle tests, orientation tests).  Failure to understand and address
 * potential numerical issues in your data *may cause infinite loops*!  (It is
 * our experience that sufficiently large point sets will eventually run afoul of
 * these problems if left alone.)  The /tolerance/ parameter is set to a
 * reasonable value in the defaults of the DelaunayTriangulation object's apply()
 * method, but bear in mind that problems may arise in your particular case.
 */
case class DelaunayTriangulation(pointSet: DelaunayPointSet, halfEdgeTable: HalfEdgeTable, tolerance: Double, debug: Boolean)
{
  /**
   * Contains the triangles of a DelaunayTriangulation
   *
   * Note: This collection will be empty if isLinear is true.
   */
  val triangleMap = new TriangleMap(halfEdgeTable)

  val predicates = new Predicates(pointSet, halfEdgeTable)
  import halfEdgeTable._
  import predicates._

  private val stitcher = new DelaunayStitcher(pointSet, halfEdgeTable)


  // def time[T](msg: String)(f: => T) = {
  //   val start = System.currentTimeMillis
  //   val v = f
  //   val end = System.currentTimeMillis
  //   println(s"[TIMING] $msg: ${java.text.NumberFormat.getIntegerInstance.format(end - start)} ms")
  //   v
  // }

  // class IterationTimer(msg: String) {
  //   private val timings = ListBuffer[Int]()
  //   private def f(i: Int) = java.text.NumberFormat.getIntegerInstance.format(i)
  //   def time[T](f: => T) = {
  //     val start = System.currentTimeMillis
  //     val v = f
  //     val end = System.currentTimeMillis
  //     timings += ((end - start).toInt)
  //     v
  //   }

  //   def report() = {
  //     var sum = 0
  //     var count = 0
  //     var min = Int.MaxValue
  //     var max = -1
  //     var countAbove1ms = 0
  //     var countAbove1s = 0
  //     var numberOfMax = 0
  //     for(timing <- timings) {
  //       sum += timing
  //       count += 1
  //       if(timing < min) { min = timing }
  //       if(max < timing) { max = timing }
  //       if(timing > 1) { countAbove1ms += 1 }
  //       if(timing > 1000) { countAbove1s += 1 }
  //     }
  //     println(s"[TIMING ITERATION] $msg:")
  //     println(s"   Total: ${f(sum)} ms")
  //     println(s"   Count: ${f(count)}")
  //     println(s"    Mean: ${sum.toDouble / count} ms")
  //     println(s"     Min: ${f(min)} ms")
  //     println(s"     Max: ${f(max)} ms")
  //     println(s" # > 1ms: ${f(countAbove1ms)}")
  //     println(s"  # > 1s: ${f(countAbove1s)}")
  //   }
  // }


  private def distinctPoints(lst: List[Int]): List[Int] = {
    import pointSet._

    @tailrec def dpInternal(l: List[Int], acc: List[Int]): List[Int] = {
      l match {
        case Nil => acc
        case i :: Nil => i :: acc
        case i :: rest => {
          val chunk = rest.takeWhile{j => getX(j) <= getX(i) + tolerance}
          if (chunk.exists { j =>
            val (xj, yj) = (getX(j), getY(j))
            val (xi, yi) = (getX(i), getY(i))
            val dx = xj - xi
            val dy = yj - yi
            math.sqrt((dx * dx) + (dy * dy)) < tolerance
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

  private val sortedVs = {
    import pointSet._
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

  // val iTimer = new IterationTimer("Trianglate function")

  private def triangulate(lo: Int, hi: Int): (Int, Boolean) = {
    // Implementation follows Guibas and Stolfi, ACM Transations on Graphics, vol. 4(2), 1985, pp. 74--123

    val n = hi - lo + 1

    if (debug) {
      println(s"\u001b[38;5;208mTriangulating [$lo..$hi]\u001b[0m")
    }

    val result =
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
          triangleMap += (v1, v2, v3) -> getFlip(p)
          (p, false)
        } else if(isCCW(v1, v3, v2)) {
          val e = createHalfEdges(v1, v3, v2)
          triangleMap += (v1, v3, v2) -> getFlip(e)
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

        // iTimer.time {
        stitcher.merge(left, isLeftLinear, right, isRightLinear, triangleMap, debug)
        // }
      }
    }

    result
  }

  private def write(path: String, txt: String): Unit = {
    import java.nio.file.{Paths, Files}
    import java.nio.charset.StandardCharsets

    Files.write(Paths.get(path), txt.getBytes(StandardCharsets.UTF_8))
  }

  var (_boundary, _isLinear) = triangulate(0, sortedVs.length - 1)
  // iTimer.report()

  /**
   * Returns a reference to the half edge on the outside of the boundary of the
   * triangulation.
   *
   * Starting from this half edge, the boundary of the triangulation can be
   * traversed.  This exterior loop will have a clockwise winding and will
   * be convex.
   */
  def boundary() = _boundary

  /**
   * Is this triangulation linear?
   *
   * If all distinct input points are collinear, this will return true.  If so,
   * the triangleMap will be empty.
   */
  def isLinear() = _isLinear

  /**
   * A correctness check which tests if two triangles which share an edge
   * overlapping.
   */
  def isUnfolded(): Boolean = {

    val bounds = Set.empty[Int]

    var e = boundary
    do {
      bounds += e
      e = getNext(e)
    } while (e != boundary)

    triangleMap.getTriangles.forall{ case (_, e) =>
      var f = e
      var ok = true
      do {
        if (!bounds.contains(getFlip(f))) {
          val v = getDest(getNext(getFlip(f)))
          ok = ok && isRightOf(f, v)
        }
        f = getNext(f)
      } while (f != e)
      ok
    }
  }

  def isUnfolded(bound: Int, lo: Int, hi: Int): Boolean = {

    val bounds = Set.empty[Int]

    var e = bound
    do {
      bounds += e
      e = getNext(e)
    } while (e != bound)

    triangleMap.getTriangles.filter { case ((a, b, c), _) =>
      (lo <= a && a <= hi) &&
      (lo <= b && b <= hi) &&
      (lo <= c && c <= hi)
    }.forall{ case (_, e) =>
      var f = e
      var ok = true
      do {
        if (!bounds.contains(getFlip(f))) {
          val v = getDest(getNext(getFlip(f)))
          ok = ok && isRightOf(f, v)
        }
        f = getNext(f)
      } while (f != e)
      ok
    }
  }

  /**
   * Outputs the triangulation to the named file as a WKT representation of a
   * MultiPolygon.
   */
  def writeWKT(wktFile: String) = {
    val indexToCoord = pointSet.getCoordinate(_)
    val mp = MultiPolygon(triangleMap.getTriangles.keys.toSeq.map{
      case (i,j,k) => Polygon(indexToCoord(i), indexToCoord(j), indexToCoord(k), indexToCoord(i))
    })
    val wktString = WKT.write(mp)
    new java.io.PrintWriter(wktFile) { write(wktString); close }
  }

  private def holeBound(vi: Int) = {
    val e0 = getFlip(edgeIncidentTo(vi))
    val b0 = HalfEdge[Int, Int](getDest(rotCCWSrc(e0)), getDest(e0))
    b0.face = Some(rotCWDest(e0))
    var last = b0

    var e = rotCWSrc(e0)
    do {
      val b = HalfEdge[Int, Int](getDest(rotCCWSrc(e)), getDest(e))
      b.face = Some(rotCWDest(e))
      last.next = b
      b.flip.next = last.flip
      last = b
      e = rotCWSrc(e)
    } while (e != e0)
    last.next = b0
    b0.flip.next = last.flip

    b0.flip
  }

  private def triangulateHole(inner: HalfEdge[Int, Int], tris: Map[(Int, Int, Int), HalfEdge[Int, Int]]): Unit = {
    val bps = ListBuffer.empty[Point]
    bps += Point.jtsCoord2Point(pointSet.getCoordinate(inner.src))

    var n = 0
    var e = inner
    do {
      n += 1
      bps += Point.jtsCoord2Point(pointSet.getCoordinate(e.vert))
      e = e.next
    } while (e != inner)

    if (n == 3) {
      tris += TriangleMap.regularizeIndex(inner.src, inner.vert, inner.next.vert) -> inner
      return ()
    }

    // find initial best point
    var best = inner.next
    while (!isCCW(inner.src, inner.vert, best.vert)) {
      best = best.next
    }

    e = best.next
    while (e.vert != inner.src && !isCCW(inner.src, inner.vert, e.vert)) {
      e = e.next
    }

    while (e.vert != inner.src) {
      if (inCircle(inner.src, inner.vert, best.vert, e.vert)) {
        best = e
        while (!isCCW(inner.src, inner.vert, best.vert)) {
          best = best.next
        }
      }
      e = e.next
      while (e.vert != inner.src && !isCCW(inner.src, inner.vert, e.vert))
      e = e.next
    }

    if (best != inner.next) {
      val te = HalfEdge[Int, Int](inner.vert, best.vert)
      te.next = best.next
      te.flip.next = inner.next
      inner.next = te
      best.next = te.flip
      best = te
      triangulateHole(te.flip, tris)
    }

    if (best.vert != inner.prev.src) {
      val te = HalfEdge[Int, Int](best.vert, inner.src)
      te.next = inner
      te.flip.next = best.next
      inner.prev.next = te.flip
      best.next = te
      triangulateHole(te.flip, tris)
    }

    tris += TriangleMap.regularizeIndex(inner.src, inner.vert, inner.next.vert) -> inner
    ()
  }

  // checks a predicate at all edges in a chain from a to b (not including b)
  private def allSatisfy(a: HalfEdge[Int, Int], b: HalfEdge[Int, Int], f: HalfEdge[Int, Int] => Boolean): Boolean = {
    var e = a
    var result = true
    do {
      result = result && f(e)
      e = e.next
    } while (result && e != b)
    result
  }

  private def link(a: HalfEdge[Int, Int], b: HalfEdge[Int, Int]): HalfEdge[Int, Int] = {
    val result = HalfEdge[Int, Int](a.src, b.vert)
    result.flip.next = a
    result.next = b.next
    a.prev.next = result
    b.next = result.flip
    result
  }

  private def retriangulateBoundaryPoint(vi: Int): (HalfEdge[Int, Int], Int, Map[(Int, Int, Int), HalfEdge[Int, Int]]) = {
    //println(s"  \u001b[38;5;55m➟ retriangulateBoundaryPoint($vi)\u001b[0m")

    val c2p = { i: Int => Point.jtsCoord2Point(pointSet.getCoordinate(i)) }
    val tris = Map.empty[(Int, Int, Int), HalfEdge[Int, Int]]

    // println("  ➟ finding bounding path")

    // Find the ends of the bounding path
    var e = getFlip(edgeIncidentTo(vi))
    while (getDest(getNext(rotCWSrc(e))) == getDest(e)) {
      e = rotCWSrc(e)
    }

    val end = getDest(rotCWSrc(e))
    e = getNext(e)

    // Build the bounding path
    val first = HalfEdge[Int, Int](getSrc(e), getDest(e))
    var last = first
    last.flip.face = Some(getFlip(e))

    val bps = ListBuffer.empty[Point]
    bps += c2p(first.src)
    bps += c2p(first.vert)

    if (getDest(e) == end) {
      // exit if there's nothing to do
      return (first, end, tris)
    }

    while (getDest(e) != end) {
      e = getNext(getFlip(getNext(e)))
      val b = HalfEdge[Int, Int](getSrc(e), getDest(e))
      b.flip.face = Some(getFlip(e))
      b.flip.next = last.flip
      last.next = b
      last = b
      bps += c2p(b.vert)
    }
    val outOfBounds = last.next

    // find convex hull segments and triangulate developed loops
    var base = first
    var best: HalfEdge[Int, Int] = null
    while (base != outOfBounds) {
      var b = base.next
      // println(s"  ➟ starting with base = ${WKT.write(Line(c2p(base.src), c2p(base.vert)))}")
      while ( b != outOfBounds) {
        // println(s"  ➟ b = ${c2p(b.vert)}")
        if (allSatisfy(base, b, { edge => isCCW(b.vert, base.src, edge.vert) })) {
          best = b
          // println(s"  ➟ found candidate = ${c2p(best.vert)}")
        }
        b = b.next
      }
      if (best == null) {
        base = base.next
      } else {
        var connector = link(base, best)
        triangulateHole(connector.flip, tris)
        base = connector.next
        best = null
      }
      
    }

    //println(s"  ➟ found triangles ${tris.keys}")
    (first.flip.next, end, tris)
  }

  private def retriangulateInteriorPoint(vi: Int) = {
    //println(s"  \u001b[38;5;208m➟ retriangulateInteriorPoint($vi)\u001b[0m")
    val tris = Map.empty[(Int, Int, Int), HalfEdge[Int, Int]]
    triangulateHole(holeBound(vi), tris)
    //println(s"  ➟ found triangles ${tris.keys}")
    tris
  }

  private def decoupleVertex(vi: Int) = {
    // remove links to original vertex
    //println(s"  ➟ disconnect original vertex $vi")
    val e0 = getFlip(edgeIncidentTo(vi))
    var e = e0
    val toKill = Set.empty[Int]
    val pts = collection.mutable.ListBuffer.empty[Point]
    do {
      triangleMap -= ((getSrc(getFlip(e)), getDest(getFlip(e)), getDest(getNext(getFlip(e)))))
      e = rotCWSrc(e)
    } while (e != e0)
    do {
      //println(s"    removing triangle ${(getSrc(getFlip(e)), getDest(getFlip(e)), getDest(getNext(getFlip(e))))}")
      pts.prepend(Point.jtsCoord2Point(pointSet.getCoordinate(getDest(e))))
      setNext(getPrev(getFlip(e)), getNext(e))
      val b = getFlip(getNext(e))
      setIncidentEdge(getDest(e), b)
      toKill += e
      e = rotCWSrc(e)
    } while (e != e0)
    val region = Line(pts)
    // println(s"    Bounding loop: ${WKT.write(region)}")

    toKill.foreach{ e => {
      //println(s"    destroying edge [${getSrc(e)} -> ${getDest(e)}] (edge ids: $e and ${getFlip(e)})")
      killEdge(getFlip(e))
      killEdge(e) } }

    removeIncidentEdge(vi)
  }

  private def removeVertexAndFill(vi: Int, tris: Map[(Int, Int, Int), HalfEdge[Int, Int]], bnd: Option[Int]): Seq[Int] = {
    val exteriorRing = ListBuffer.empty[Int]

    decoupleVertex(vi)

    // in the event of a boundary with no fill triangles, set the boundary 
    // reference in case we destroyed the old boundary edge (happens when 
    // corner points are deleted)
    if (bnd != None) {
      _boundary = getFlip(bnd.get)
    }
  
    // merge triangles
    //println("  ➟ merge new triangles")
    val edges = Map.empty[(Int, Int), Int]
    tris.foreach { case (ix, h) => {
      val v1 = h.src
      val v2 = h.vert
      val v3 = h.next.vert

      //println(s"    Found triangle ${(v1, v2, v3)} [had index: $ix]")

      var newtri = getFlip(createHalfEdges(v1, v2, v3))
      triangleMap += newtri

      //println(s"    created triangle ${(getSrc(newtri), getDest(newtri), getDest(getNext(newtri)))}")

      var b = h

      do {
        assert (getSrc(newtri) == b.src && getDest(newtri) == b.vert)
        b.flip.face match {
          case Some(opp) =>
            //println(s"    joining to boundary triangle ${(getSrc(opp), getDest(opp), getDest(getNext(opp)))}")
            exteriorRing += getDest(getNext(opp))
            join(newtri, opp)
          case None =>
            edges.get(b.vert -> b.src) match {
              case Some(opp) => 
                //println(s"    joining to fill triangle ${(getSrc(opp), getDest(opp), getDest(getNext(opp)))}")
                edges -= (b.vert -> b.src)
                join(newtri, opp)
              case None =>
                //println(s"    storing edge [${b.src} -> ${b.vert}]")
                edges += (b.src, b.vert) -> newtri
            }
        }

        newtri = getNext(newtri)
        b = b.next
      } while (b != h)
    }}

    if (!edges.isEmpty) {
      //println(s"    edges remain at end of triangulation: $edges")
      _boundary = getFlip(edges.head._2)
      //println(s"    boundary edge now refers to [${getSrc(boundary)} -> ${getDest(boundary)}] (ID: $boundary)")
    }

    exteriorRing
  }

  /** A function to remove a vertex from a DelaunayTriangulation that preserves
   *  the Delaunay property for all newly created fill triangles.
   */
  def deletePoint(vi: Int) = {
    //println(s"Removing point $vi")
    //println(s"  ➟ Neighbor set is ${neighborsOf(vi)}")

    val boundvs = Set.empty[Int]
    var e = boundary
    do {
      boundvs += getDest(e)
      e = getNext(e)
    } while (e != boundary)

    val (tris, bnd) =
      if (boundvs.contains(vi)) {
        //println("  ➟ boundary point")
        val (bnd, _, tris) = retriangulateBoundaryPoint(vi)
        (tris, bnd.flip.face)
      } else {
        //println("  ➟ interior point")
        (retriangulateInteriorPoint(vi), None)
      }

    removeVertexAndFill(vi, tris, bnd)
    ()
  }

  /**
   * A correctness check for the triangulation.
   *
   * Ensures that the mesh is correctly built with no obvious topological errors
   * in the mesh.
   */
  def isMeshValid(): Boolean = {
    val edges = Map.empty[(Int, Int), Int]
    val triedges = Set.empty[Int]
    var result = true

    var e = boundary
    do {
      edges += (getSrc(e) -> getDest(e)) -> e
      e = getNext(e)
    } while (e != boundary)

    val triverts = Set.empty[Int]
    triangleMap.triangleVertices.foreach { case (i, j, k) =>
      triverts += i
      triverts += j
      triverts += k
    }

    triangleMap.getTriangles.foreach { case ((i1, i2, i3), t0) => 
      var t = t0
      var i = 0

      if (Set(i1, i2, i3) != Set(getSrc(t), getDest(t), getDest(getNext(t)))) {
        println(s"Triangle ${(i1, i2, i3)} references loop over [${getSrc(t)}, ${getDest(t)}, ${getDest(getNext(t))}, ${getDest(getNext(getNext(t)))}, ...]")
        result = false
      }

      do {
        triedges += t
        edges.get(getSrc(t) -> getDest(t)) match {
          case None =>
            edges.get(getDest(t) -> getSrc(t)) match {
              case None =>
                edges += (getSrc(t) -> getDest(t)) -> t
              case Some(s) =>
                if (getFlip(t) != s || getFlip(s) != t) {
                  println(s"Edges [${getSrc(t)} -> ${getDest(t)}] and [${getSrc(s)} -> ${getDest(s)}] are not mutual flips!")
                  result = false
                }
            }
          case Some(s) =>
            println(s"In triangle ${(i1, i2, i3)}: already encountered edge [${getSrc(t)} -> ${getDest(t)}]!")
            print("   first in ") ; showLoop(s)
            print("   and then in ") ; showLoop(t)
            result = false
        }
        i += 1
        t = getNext(t)
      } while (t != t0)
      if (i != 3) {
        println(s"Edge [${getSrc(t0)} -> ${getDest(t0)}] does not participate in triangle ${(i1, i2, i3)}! (loop of length $i)")
      }
    }

    allVertices.foreach{ v => 
      val t = edgeIncidentTo(v)
      if (!triedges.contains(t)) {
        println(s"edgeIncidentTo($v) refers to non-interior or stale edge [${getSrc(t)} -> ${getDest(t)}] (ID: ${t})")
        result = false
      }
    }

    if (allVertices != triverts) {
      val vertsNotInTris = allVertices.toSet -- triverts
      val trivertsNotInEdges = triverts -- allVertices
      if (vertsNotInTris nonEmpty) {
        println(s"The vertices $vertsNotInTris are not contained in triangles but have incident edges")
      }
      if (trivertsNotInEdges nonEmpty) {
        println(s"The vertices $trivertsNotInEdges appear in triangles but have no incident edges")
      }
      result = false
    }

    result
  }

  /**
   * Provides a text-based interactive interface to explore the structure of a
   * triangulation.
   */
  def navigate(): Unit = {
    val cmds = collection.immutable.Map[Char, (String, Int => Int)](
      'i' -> ("mesh information", { e =>
        println(s"Number of vertices:  ${allVertices.size}")
        println(s"Number of triangles: ${triangleMap.getTriangles.size}")
        print(s"List of triangles:   ")
        triangleMap.triangleVertices.foreach{ t => print(s"$t ") }
        println
        e }),
      'x' -> ("export to WKT file", { e =>
        val name = scala.io.StdIn.readLine("Enter file name: ")
        writeWKT(name)
        e })
    )

    halfEdgeTable.navigate(boundary, pointSet.getCoordinate(_), cmds)
  }

  /**
   * Simplifies a triangulation.
   *
   * This method will remove a specified number of vertices from the
   * triangulation, where the next vertex to be removed causes the least amount
   * of error to be introduced to the surface.  This function assumes that all
   * coordinates have a valid z component, and therefore, the triangulation is
   * interpretable as a height field.
   *
   * Implementation is based on paper by Garland, Michael, and Paul S. Heckbert.
   * "Surface simplification using quadric error metrics." Proceedings of the
   * 24th annual conference on Computer graphics and interactive techniques. ACM
   * Press/Addison-Wesley Publishing Co., 1997.
   */
  def decimate(nRemove: Int) = {
    //println(s"\n\u001b[1mStarting to decimate $nRemove points...\u001b[0m")

    val trans = pointSet.getCoordinate(_)

    def constructPQEntry(vi: Int) = {
      val (quadric, tris, bnd) = if (onBoundary(vi, boundary)) {
        val (bound, end, tris) = retriangulateBoundaryPoint(vi)
        val quadric = QuadricError.facetMatrix(tris.keys, trans).add(QuadricError.edgeMatrix(bound, end, trans))
        (quadric, tris, bound.flip.face)
      } else {
        val tris = retriangulateInteriorPoint(vi)
        val quadric = QuadricError.facetMatrix(tris.keys, trans)
        (quadric, tris, None)
      }
      val pt = trans(vi)
      val v = MatrixUtils.createRealVector(Array(pt.x, pt.y, pt.z, 1))
      val score = v dotProduct ( quadric operate v)
      (score, vi, quadric, tris, bnd)
    }

    // if (isMeshValid)
    //   println("  \u001b[32m➟ Initial mesh is valid\u001b[0m")
    // else
    //   println("  \u001b[31m➟ Initial mesh is NOT valid\u001b[0m")

    // build priority queue
    // println(s"  \u001b[32m➟ Applying initial score to vertices\u001b[0m")
    var pq = PriorityQueue.empty[(Double, Int, RealMatrix, Map[(Int, Int, Int), HalfEdge[Int, Int]], Option[Int])](
      Ordering.by((_: (Double, Int, RealMatrix, Map[(Int, Int, Int), HalfEdge[Int, Int]], Option[Int]))._1).reverse
    )
    allVertices.foreach { vi: Int => pq.enqueue(constructPQEntry(vi)) }

    // iterate
    cfor(0)(i => i < nRemove && !pq.isEmpty, _ + 1) { i =>
      val (score, vi, _, tris, bnd) = pq.dequeue

      // println(s"\u001b[1m[Iteration $i] Removing vertex $vi with score = ${score}\u001b[0m")
      // navigate

      // if (onBoundary(vi, boundary))
      //   println("  ➟ point is on boundary")

      // remove vertex and record all vertices that require updating
      val nbhd = neighborsOf(vi).toSet ++ removeVertexAndFill(vi, tris, bnd)

      // println("  ➟ checking mesh validity")
      // if (isMeshValid)
      //   println("  \u001b[32m➟ mesh is valid\u001b[0m")
      // else
      //   println("  \u001b[31m➟ mesh is NOT valid\u001b[0m")

      // update neighbor entries from pqueue
      //println(s"  ➟ update neighbors [$nbhd]")
      pq = pq.filter { case (_, ix, _, _, _) => !nbhd.contains(ix) }
      //println(s"    left alone patches for vertices ${pq.map(_._2)}")
      nbhd.foreach{ neighbor => pq.enqueue(constructPQEntry(neighbor)) }

      //writeWKT(s"simplify${i}.wkt")
    }
  }

}

object DelaunayTriangulation {
  def apply(pointSet: DelaunayPointSet, tolerance: Double = 1e-8, debug: Boolean = false) = {
    val initialSize = 2 * (3 * pointSet.length - 6)
    new DelaunayTriangulation(pointSet, new HalfEdgeTable(initialSize), tolerance, debug)
  }
}
