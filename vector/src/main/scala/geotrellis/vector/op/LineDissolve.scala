package geotrellis.vector.op

import geotrellis.vector._

import spire.syntax.cfor._

import scala.collection._

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

/** This object merely holds its method `dissolve` in a closure alongside its helper `order` */
object LineDissolve {

  /** A private helper function for ensuring that the ordering of segments is preserved. */
  private def order(p1: jts.Coordinate, p2: jts.Coordinate): (Double, Double, Double, Double) = {
    if (p1.x < p2.x)      { (p1.x, p1.y, p2.x, p2.y) }
    else if (p1.x > p2.x) { (p2.x, p2.y, p1.x, p1.y) }
    else if (p1.y < p2.y) { (p1.x, p1.y, p2.x, p2.y) }
    else                  { (p2.x, p2.y, p1.x, p1.y) }
  }

  /** Dissolve a complex of line segments into constituent, unique line segments
    *
    * @param lines the lines to dissolve
    * @return all unique, constituent line segments
    */
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
      val points = line.jtsGeom.clone.asInstanceOf[jts.Geometry].getCoordinates

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

}
