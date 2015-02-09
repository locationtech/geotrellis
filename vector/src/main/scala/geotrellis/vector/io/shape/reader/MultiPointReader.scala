package geotrellis.vector.io.shape.reader

import geotrellis.vector._
import com.vividsolutions.jts.{geom => jts}
import spire.syntax.cfor._

import scala.collection.mutable
import java.nio.ByteBuffer

import GeomFactory._

class MultiPointReader(dimensions: Int) extends GeometryReader(dimensions) {
  def readGeometry(byteBuffer: ByteBuffer): Geometry = {
    val count = byteBuffer.getInt
    val coords = Array.ofDim[jts.Coordinate](count)
    cfor(0)(_ < count, _ + 1) { i =>
      coords(i) = new jts.Coordinate(byteBuffer.getDouble, byteBuffer.getDouble)
    }
    MultiPoint(factory.createMultiPoint(coords))
  }
}

