package geotrellis.vector.io.shape.reader

import geotrellis.vector._
import com.vividsolutions.jts.{geom => jts}
import spire.syntax.cfor._

import scala.collection.mutable
import java.nio.ByteBuffer

import GeomFactory._

class PointReader(dimensions: Int) extends GeometryReader(dimensions, hasBoundingBox = false) {
  def readGeometry(byteBuffer: ByteBuffer): Geometry = {
    Point(byteBuffer.getDouble, byteBuffer.getDouble)
  }
}

