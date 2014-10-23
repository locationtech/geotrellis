package geotrellis.vector.op

import geotrellis.vector._

import spire.syntax.cfor._

import scala.collection._
import java.lang.{ Double => JDouble }

object LineDissolve {

  class LineSegment(val p1: Point, val p2: Point) {
    def toLine: Line = Line(p1, p2)

    override def hashCode = {
      val b0 = JDouble.doubleToLongBits(p1.x) ^ (
        JDouble.doubleToLongBits(p1.y) * 31)
      val hash0 = b0.toInt ^ (b0 >> 32).toInt

      val b1 = JDouble.doubleToLongBits(p2.x) ^ (
        JDouble.doubleToLongBits(p2.y) * 31)
      val hash1 = b1.toInt ^ (b1 >> 32).toInt

      hash0 ^ hash1
    }

  }

  def dissolve(lines: Traversable[Line]): Seq[Line] = {
    val lineArray = lines.toArray
    var size = 0
    cfor(0)(_ < lineArray.size, _ + 1) { i =>
      size += lineArray(i).points.size
    }

    val hashSet = new java.util.HashSet[LineSegment](size + 1000, 1.0f)

    cfor(0)(_ < lineArray.size, _ + 1) { i =>
      val line = lineArray(i)
      val points = line.points

      cfor(0)(_ < points.size - 1, _ + 1) { j =>
        val p1 = points(j)
        val p2 = points(j + 1)
        if(p1 != p2) {
          hashSet.add(new LineSegment(p1, p2))
        }
      }
    }

    val arr = Array.ofDim[Line](hashSet.size)
    val it = hashSet.iterator

    var idx = 0

    while (it.hasNext) {
      arr(idx) = it.next.toLine
      idx += 1
    }

    arr
  }

  trait Implicits {
    implicit class LineDissolveWrapper(val lines: Traversable[Line]) {
      def dissolve() = LineDissolve.dissolve(lines)
    }
  }

}
