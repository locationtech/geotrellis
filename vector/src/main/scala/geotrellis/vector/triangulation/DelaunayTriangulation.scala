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
}

object DelaunayTriangulation {
  def apply(pointSet: DelaunayPointSet, debug: Boolean = false) = {
    val initialSize = 2 * (3 * pointSet.length - 6)
    new DelaunayTriangulation(pointSet, new HalfEdgeTable(initialSize), debug)
  }
}
