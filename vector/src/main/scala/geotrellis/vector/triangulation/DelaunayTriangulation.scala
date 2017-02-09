package geotrellis.vector.triangulation

// for debugging
import geotrellis.vector._
import geotrellis.vector.io.wkt.WKT

import scala.annotation.tailrec

case class DelaunayTriangulation(pointSet: DelaunayPointSet, halfEdgeTable: HalfEdgeTable, debug: Boolean)
{
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
  //   private val timings = scala.collection.mutable.ListBuffer[Int]()
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


  def distinctPoints(lst: List[Int]): List[Int] = {
    import pointSet._

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
    //println("Finding distinct points")
    distinctPoints(s)
    .toArray
  }

  // val iTimer = new IterationTimer("Trianglate function")

  def triangulate(lo: Int, hi: Int): (Int, Boolean) = {
    // Implementation follows Guibas and Stolfi, ACM Transations on Graphics, vol. 4(2), 1985, pp. 74--123

    val n = hi - lo + 1

    //println(s"Triangulating [$lo..$hi]")

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
          triangleMap += (v1, v2, v3) -> p
          (p, false)
        } else if(isCCW(v1, v3, v2)) {
          val e = createHalfEdges(v1, v3, v2)
          triangleMap += (v1, v3, v2) -> e
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
        stitcher.merge(left, isLeftLinear, right, isRightLinear, triangleMap)
        // }
      }
    }

    result
  }

  def write(path: String, txt: String): Unit = {
    import java.nio.file.{Paths, Files}
    import java.nio.charset.StandardCharsets

    Files.write(Paths.get(path), txt.getBytes(StandardCharsets.UTF_8))
  }

  val (boundary, isLinear) = triangulate(0, sortedVs.length - 1)
  // iTimer.report()

  def isUnfolded(): Boolean = {

    val bounds = collection.mutable.Set.empty[Int]

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

    val bounds = collection.mutable.Set.empty[Int]

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

  def writeWKT(wktFile: String) = {
    val indexToCoord = pointSet.getCoordinate(_)
    val mp = MultiPolygon(triangleMap.getTriangles.keys.toSeq.map{
      case (i,j,k) => Polygon(indexToCoord(i), indexToCoord(j), indexToCoord(k), indexToCoord(i))
    })
    val wktString = WKT.write(mp)
    new java.io.PrintWriter(wktFile) { write(wktString); close }
  }

  def holeBound(vi: Int) = {
    println("  ➟ holeBound()")

    val e0 = getFlip(edgeIncidentTo(vi))
    val b0 = HalfEdge[Int, Int](getDest(rotCCWSrc(e0)), getDest(e0))
    if (getDest(getNext(e0)) == rotCCWSrc(e0))
      b0.face = Some(rotCWDest(e0))
    var last = b0

    var e = rotCWSrc(e0)
    do {
      val b = HalfEdge[Int, Int](getDest(rotCCWSrc(e)), getDest(e))
      if (getDest(getNext(e)) == rotCCWSrc(e))
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

  def triangulateHole(inner: HalfEdge[Int, Int], tris: collection.mutable.Map[(Int, Int, Int), HalfEdge[Int, Int]]): Unit = {
    println("  ➟ triangulateHole()")

    val bps = collection.mutable.ListBuffer.empty[Point]
    bps += Point.jtsCoord2Point(pointSet.getCoordinate(inner.src))

    var n = 0
    var e = inner
    do {
      n += 1
      bps += Point.jtsCoord2Point(pointSet.getCoordinate(e.vert))
      e = e.next
    } while (e != inner)
    println(WKT.write(Polygon(bps)))

    if (n == 3) {
      tris += TriangleMap.regularizeIndex(inner.src, inner.vert, inner.next.vert) -> inner
      return ()
    }
    println(s"  ➟ bounding loop has $n points")

    // find initial best point
    var best = inner.next
    print(s"  ➟ find initial best vertex")
    while (!isCCW(inner.src, inner.vert, best.vert)) {
      best = best.next
    }
    println(s" = ${pointSet.getCoordinate(best.vert)}")

    e = best.next
    print(s"  ➟ find initial candidate")
    while (e.vert != inner.src && !isCCW(inner.src, inner.vert, e.vert)) {
      e = e.next
    }
    println(s" = ${pointSet.getCoordinate(e.vert)}")

    println(s"  ➟ check for better candidates")
    while (e.vert != inner.src) {
      if (inCircle(inner.src, inner.vert, best.vert, e.vert)) {
        print(s"  ➟ found better")
        best = e
        println(s" = ${pointSet.getCoordinate(best.vert)}")
        while (!isCCW(inner.src, inner.vert, best.vert)) {
          best = best.next
        }
      }
      e = e.next
      while (e.vert != inner.src && !isCCW(inner.src, inner.vert, e.vert))
      e = e.next
    }
    println(WKT.write(Polygon(Point.jtsCoord2Point(pointSet.getCoordinate(inner.src)), Point.jtsCoord2Point(pointSet.getCoordinate(inner.vert)), Point.jtsCoord2Point(pointSet.getCoordinate(best.vert)), Point.jtsCoord2Point(pointSet.getCoordinate(inner.src)))))

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
  def allSatisfy(a: HalfEdge[Int, Int], b: HalfEdge[Int, Int], f: HalfEdge[Int, Int] => Boolean): Boolean = {
    //println("  ➟ allSatisfy()")

    var e = a
    var result = true
    do {
      result = result && f(e)
      e = e.next
    } while (result && e != b)
    result
  }

  def link(a: HalfEdge[Int, Int], b: HalfEdge[Int, Int]): HalfEdge[Int, Int] = {
    println("  ➟ link()")

    val result = HalfEdge[Int, Int](a.src, b.vert)
    result.flip.next = a
    result.next = b.next
    a.prev.next = result
    b.next = result.flip
    result
  }

  def retriangulateBoundaryPoint(vi: Int): (HalfEdge[Int, Int], Int, collection.mutable.Map[(Int, Int, Int), HalfEdge[Int, Int]]) = {
    println("  ➟ retriangulateBoundaryPoint()")

    val c2p = { i: Int => Point.jtsCoord2Point(pointSet.getCoordinate(i)) }
    val tris = collection.mutable.Map.empty[(Int, Int, Int), HalfEdge[Int, Int]]

    println("  ➟ finding bounding path")

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

    val bps = collection.mutable.ListBuffer.empty[Point]
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

    println(WKT.write(Line(bps)))

    // find convex hull segments and triangulate developed loops
    var base = first
    var best: HalfEdge[Int, Int] = null
    while (base != outOfBounds) {
      var b = base.next
      println(s"  ➟ starting with base = ${WKT.write(Line(c2p(base.src), c2p(base.vert)))}")
      while ( b != outOfBounds) {
        println(s"  ➟ b = ${c2p(b.vert)}")
        if (allSatisfy(base, b, { edge => isCCW(b.vert, base.src, edge.vert) })) {
          best = b
          println(s"  ➟ found candidate = ${c2p(best.vert)}")
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
    (first.flip.next, end, tris)
  }

  def retriangulateInteriorPoint(vi: Int) = {
    println("  ➟ retriangulateInteriorPoint")
    val tris = collection.mutable.Map.empty[(Int, Int, Int), HalfEdge[Int, Int]]
    triangulateHole(holeBound(vi), tris)
    tris
  }

  def makeFillPatch(vi: Int) = {
    println(s"Writing fill patch for vertex $vi")

    val boundvs = collection.mutable.Set.empty[Int]
    var e = boundary
    do {
      boundvs += getDest(e)
      e = getNext(e)
    } while (e != boundary)

    val tris =
      if (boundvs.contains(vi)) {
        println("  ➟ boundary point")
        val (_, _, tris) = retriangulateBoundaryPoint(vi)
        tris
      } else {
        println("  ➟ interior point")
        retriangulateInteriorPoint(vi)
      }

    val mp = MultiPolygon(tris.map{ case ((a,b,c), _) => {
      val pa = Point.jtsCoord2Point(pointSet.getCoordinate(a))
      val pb = Point.jtsCoord2Point(pointSet.getCoordinate(b))
      val pc = Point.jtsCoord2Point(pointSet.getCoordinate(c))

      Polygon(pa, pb, pc, pa)
    }})

    new java.io.PrintWriter(s"patch${vi}.wkt") { write(WKT.write(mp)); close }

    tris
  }

  /** A function to remove a vertex from a DelaunayTriangulation that adheres to
   *  the Delaunay property for all newly created fill triangles.
   */
  def deletePoint(vi: Int) = {
    println(s"Removing point $vi")

    // Generate patch
    val boundvs = collection.mutable.Set.empty[Int]
    var e = boundary
    do {
      boundvs += getDest(e)
      e = getNext(e)
    } while (e != boundary)

    val tris =
      if (boundvs.contains(vi)) {
        println("  ➟ boundary point")
        retriangulateBoundaryPoint(vi)._3
      } else {
        println("  ➟ interior point")
        retriangulateInteriorPoint(vi)
      }

    // remove links to original vertex
    println("  ➟ disconnect original vertex")
    val e0 = getFlip(edgeIncidentTo(vi))
    e = e0
    do {
      setNext(getPrev(getFlip(e)), getNext(e))
      triangleMap -= ((getSrc(getFlip(e)), getDest(getFlip(e)), getDest(getNext(getFlip(e)))))
      e = rotCWSrc(e)
    } while (e != e0)

    removeIncidentEdge(vi)
    
    // merge triangles
    println("  ➟ merge new triangles")
    val edges = collection.mutable.Map.empty[(Int, Int), Int]
    tris.foreach { case (_, h) => {
      val v1 = h.src
      val v2 = h.vert
      val v3 = h.next.vert

      val newtri = getFlip(createHalfEdges(v1, v2, v3))
      triangleMap.+=(newtri)

      var edge = newtri
      var b = h

      do {
        assert (getSrc(edge) == b.src && getDest(edge) == b.vert)
        b.flip.face match {
          case Some(opp) =>
            join(edge, opp)
          case None =>
            edges.get(b.vert -> b.src) match {
              case Some(opp) => 
                join(edge, opp)
              case None =>
                edges += (b.src, b.vert) -> edge
            }
        }

        edge = getNext(edge)
        b = b.next
      } while (b != h)
    }}
  }

  def decimate(nRemove: Int) = {
    val boundvs = collection.mutable.Set.empty[Int]
    var e = boundary
    do {
      boundvs += getDest(e)
      e = getNext(e)
    } while (e != boundary)

    ???     
  }

}

object DelaunayTriangulation {
  def apply(pointSet: DelaunayPointSet, debug: Boolean = false) = {
    val initialSize = 2 * (3 * pointSet.length - 6)
    new DelaunayTriangulation(pointSet, new HalfEdgeTable(initialSize), debug)
  }
}
