package geotrellis.vector

import collection._

import spire.syntax.cfor._

import java.lang.{ Double => JDouble }

object FastLineDeDuplication {

  class LineSegment(val line: (Point, Point)) {

    override def hashCode = {
      val b0 = JDouble.doubleToLongBits(line._1.x) ^ (
        JDouble.doubleToLongBits(line._1.y) * 31)
      val hash0 = b0.toInt ^ (b0 >> 32).toInt

      val b1 = JDouble.doubleToLongBits(line._2.x) ^ (
        JDouble.doubleToLongBits(line._2.y) * 31)
      val hash1 = b1.toInt ^ (b1 >> 32).toInt

      hash0 ^ hash1
    }

  }

  implicit class LineDeDuplication(val lines: Seq[Line]) {

    def deDuplicate: Seq[(Point, Point)] = {
      var size = 0
      cfor(0)(_ < lines.size, _ + 1) { i =>
        size += lines(i).points.size
      }

      val hashSet = new java.util.HashSet[LineSegment](size + 1000, 1.0f)

      cfor(0)(_ < lines.size, _ + 1) { i =>
        val line = lines(i)
        val points = line.points

        cfor(0)(_ < points.size - 1, _ + 1) { j =>
          hashSet.add(new LineSegment((points(j), points(j + 1))))
        }
      }

      val arr = Array.ofDim[(Point, Point)](hashSet.size)
      val it = hashSet.iterator

      var idx = 0

      while (it.hasNext) {
        arr(idx) = it.next.line
        idx += 1
      }

      arr
    }

  }

}
