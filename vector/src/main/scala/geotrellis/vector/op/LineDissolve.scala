package geotrellis.vector.op

import geotrellis.vector._

import spire.syntax.cfor._

import scala.collection._
import java.lang.{ Double => JDouble }

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

object LineDissolve {

  private def order(p1: jts.Coordinate, p2: jts.Coordinate): (Double, Double, Double, Double) = {
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

    (x1, y1, x2, y2)
  }


  def dissolve(lines: Traversable[Line]): Seq[Line] = {
    val segments = mutable.ListBuffer[Line]()

    val lineArray = lines.toArray
    var size = 0
    cfor(0)(_ < lineArray.size, _ + 1) { i =>
      size += lineArray(i).vertexCount
    }

    val ids = new java.util.HashSet[(Double, Double, Double, Double)]( (size + 1) / 2, 1.0f)

    cfor(0)(_ < lineArray.size, _ + 1) { i =>
      val line = lineArray(i)
      val points = line.jtsGeom.getCoordinates

      cfor(0)(_ < points.size - 1, _ + 1) { j =>
        val p1 = points(j)
        val p2 = points(j + 1)
        if(p1.x != p2.x || p1.y != p2.y) {
          if(ids.add(order(p1, p2))) {
            segments += Line(factory.createLineString(Array(p1, p2)))
          }
        }
      }
    }

    segments
  }

  trait Implicits {
    implicit class LineDissolveWrapper(val lines: Traversable[Line]) {
      def dissolve() = LineDissolve.dissolve(lines)
    }
  }

}
