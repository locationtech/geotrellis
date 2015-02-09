package geotrellis.vector.io.shape.reader

import geotrellis.vector._
import com.vividsolutions.jts.{geom => jts}
import spire.syntax.cfor._

import scala.collection.mutable
import java.nio.ByteBuffer

import GeomFactory._

class MultiLineReader(dimensions: Int) extends GeometryReader(dimensions) {
  def readGeometry(byteBuffer: ByteBuffer): Geometry = {
    val lineCount = byteBuffer.getInt
    val pointCount = byteBuffer.getInt

    val lines = Array.ofDim[jts.LineString](lineCount)
    val lineLengths = Array.ofDim[Int](lineCount)



    var startPoint = byteBuffer.getInt // First offset
    cfor(0)(_ < lineCount, _ + 1) { lineIndex =>
      val endPoint = 
        if(lineIndex == lineCount - 1) pointCount 
        else byteBuffer.getInt

      lineLengths(lineIndex) = endPoint - startPoint
      startPoint = endPoint
    }

    cfor(0)(_ < lineCount, _ + 1) { lineIndex =>
      val len = lineLengths(lineIndex)
      val coords = Array.ofDim[jts.Coordinate](len)
      cfor(0)(_ < len, _ + 1) { pointIndex =>
        val x = byteBuffer.getDouble
        val y = byteBuffer.getDouble
        coords(pointIndex) = new jts.Coordinate(x, y)
      }

      lines(lineIndex) = factory.createLineString(coords)
    }

    MultiLine(factory.createMultiLineString(lines))
  }
}

