package geotrellis.vector.op

import geotrellis.vector._

import spire.syntax.cfor._

import scala.collection._
import java.lang.{ Double => JDouble }

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

object LineDissolve {

  class LineSegment(val p1: jts.Coordinate, val p2: jts.Coordinate) {
    def toLine: Line = 
      Line(factory.createLineString(Array(p1, p2)))

    override val hashCode = {
      var x1 = 0.0
      var y1 = 0.0
      var x2 = 0.0
      var y2 = 0.0

      if(p1.x < p2.x) { x1 = p1.x ; y1 = p1.y ; x2 = p2.x ; y2 = p2.y }
      else if(p1.x > p2.x) { x1 = p2.x ; y1 = p2.y ; x2 = p1.x ; y2 = p1.y }
      else {
        if(p1.y < p2.y) { x1 = p1.x ; y1 = p1.y ; x2 = p2.x ; y2 = p2.y }
        else { x1 = p2.x ; y1 = p2.y ; x2 = p1.x ; y2 = p1.y }
      }

      val b0 = JDouble.doubleToLongBits(x1) ^ (
        JDouble.doubleToLongBits(y1) * 31)
      val hash0 = b0.toInt ^ (b0 >> 32).toInt

      val b1 = JDouble.doubleToLongBits(x2) ^ (
        JDouble.doubleToLongBits(y2) * 31)
      val hash1 = b1.toInt ^ (b1 >> 32).toInt

      hash0 ^ hash1
    }

  }

  def dissolve(lines: Traversable[Line]): Seq[Line] = {
    val lineArray = lines.toArray
    var size = 0
    cfor(0)(_ < lineArray.size, _ + 1) { i =>
      size += lineArray(i).vertexCount
    }

    val hashSet = new java.util.HashSet[LineSegment](size + 1000, 1.0f)

    cfor(0)(_ < lineArray.size, _ + 1) { i =>
      val line = lineArray(i)
      val points = line.jtsGeom.getCoordinates

      cfor(0)(_ < points.size - 1, _ + 1) { j =>
        val p1 = points(j)
        val p2 = points(j + 1)
        if(p1.x != p2.x || p1.y != p2.y) {
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
