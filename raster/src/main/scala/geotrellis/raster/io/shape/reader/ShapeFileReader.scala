package geotrellis.raster.io.shape.reader

import geotrellis.raster.io.Filesystem

import geotrellis.vector._

import java.nio.{ByteBuffer, ByteOrder}

import collection.mutable.{ArrayBuffer, ListBuffer}

import spire.syntax.cfor._

case class MalformedShapeFileException(msg: String) extends RuntimeException(msg)

object ShapeFileReader {

  def apply(path: String): ShapeFileReader =
    if (path.endsWith(".shp")) apply(Filesystem.slurp(path))
    else throw new MalformedShapeFileException("Bad file ending (must be .shp).")

  def apply(bytes: Array[Byte]): ShapeFileReader =
    new ShapeFileReader(ByteBuffer.wrap(bytes, 0, bytes.size))

}

object ShapeType {

  val PointType = 1
  val MultiLineType = 3
  val PolygonType = 5
  val MultiPointType = 8
  val PointZType = 11
  val MultiLineZType = 13
  val PolygonZType = 15
  val MultiPointZType = 18
  val PointMType = 21
  val MultiLineMType = 23
  val PolygonMType = 25
  val MultiPointMType = 28
  val MultiPolygonType = 31

  val ValidValues = Set(1, 3, 5, 8, 11, 13, 15, 18, 21, 23, 25, 28, 31)

}

object MultiPolygonPartType {

  val TriangleStrip = 0
  val TriangleFan = 1
  val OuterRing = 2
  val InnerRing = 3
  val FirstRing = 4
  val Ring = 5

}

class ShapeFileReader(byteBuffer: ByteBuffer) extends ShapeHeaderReader {

  import ShapeType._

  private val boundingBox = Array.ofDim[Double](8)

  lazy val read: ShapeFile = {
    val boundingBox = readHeader(byteBuffer)

    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    val recordBuffer = ArrayBuffer[ShapeRecord]()
    while (byteBuffer.remaining > 0) recordBuffer += readRecord

    ShapeFile(recordBuffer.toArray, boundingBox)
  }

  private def readRecord: ShapeRecord = {
    byteBuffer.order(ByteOrder.BIG_ENDIAN)
    val nr = byteBuffer.getInt
    val size = byteBuffer.getInt

    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    byteBuffer.getInt match {
      case PointType => readPointRecord
      case MultiPointType => readMultiPointRecord
      case MultiLineType => readMultiLineRecord
      case PolygonType => readPolygonRecord
      case PointMType => readPointMRecord
      case MultiPointMType => readMultiPointMRecord
      case MultiLineMType => readMultiLineMRecord
      case PolygonMType => readPolygonMRecord
      case PointZType => readPointZRecord
      case MultiPointZType => readMultiPointZRecord
      case MultiLineZType => readMultiLineZRecord
      case PolygonZType => readPolygonZRecord
      case MultiPolygonType => readMultiPolygonRecord
      case malformedShapeType => throw new MalformedShapeFileException(
        s"""Malformed shape type: $malformedShapeType for record no: $nr.
          Note that this reader doesn't support null types.""")
    }

  }

  private def readPoint = Point(byteBuffer.getDouble, byteBuffer.getDouble)

  private def readPoints(): Array[Point] = readPoints(byteBuffer.getInt)

  private def readPoints(numPoints: Int): Array[Point] = {
    val points = Array.ofDim[Point](numPoints)

    cfor(0)(_ < numPoints, _ + 1) { i =>
      points(i) = readPoint
    }

    points
  }

  private def readMultiPoint = MultiPoint(readPoints)

  private def readLines = {
    val numParts = byteBuffer.getInt
    val numPoints = byteBuffer.getInt

    val lines = Array.ofDim[Array[Point]](numParts)
    var start = { byteBuffer.get; byteBuffer.get }
    lines(0) = readPoints(start)

    cfor(1)(_ < numParts - 1, _ + 1) { i =>
      val offset = byteBuffer.get
      val size = offset - start
      start = offset
      lines(i) = readPoints(size)
    }

    lines
  }

  private def readMultiLine = {
    val lines = readLines
    val res = Array.ofDim[Line](lines.size)
    cfor(0)(_ < lines.size, _ + 1) { i =>
      res(i) = Line(lines(i))
    }

    MultiLine(res)
  }

  private def readPolygon = {
    val lines = readLines

    var yMaxLineIndex = -1
    var yMax = Double.NegativeInfinity

    cfor(0)(_ < lines.size, _ + 1) { i =>
      cfor(0)(_ < lines(i).size, _ + 1) { j =>
        val y = lines(i)(j).y
        if (y > yMax) {
          yMaxLineIndex = i
          yMax = y
        }
      }
    }

    var idx = 0
    val holes = Array.ofDim[Line](lines.size - 1)
    cfor(0)(_ < lines.size + 1, _ + 1) { i =>
      if (yMaxLineIndex != i) {
        holes(idx) = Line(lines(i))
        idx += 1
      }
    }

    Polygon(Line(lines(yMaxLineIndex)), holes)
  }

  private def moveByteBufferForward(steps: Int) =
    byteBuffer.position(byteBuffer.position + steps)

  private def readPointRecord(forwardSteps: Int): PointRecord = {
    val res = PointRecord(readPoint)

    moveByteBufferForward(forwardSteps)

    res
  }

  private def readPointRecord: PointRecord = readPointRecord(0)

  private def readPointMRecord: PointRecord = readPointRecord(8)

  private def readPointZRecord: PointRecord = readPointRecord(16)

  private def readMultiPointRecord(forwardStepsMult: Int): MultiPointRecord = {
    moveByteBufferForward(32)

    val multiPoint = readMultiPoint

    moveByteBufferForward((16 + 8 * multiPoint.vertexCount) * forwardStepsMult)

    MultiPointRecord(multiPoint)
  }

  private def readMultiPointRecord: MultiPointRecord = readMultiPointRecord(0)

  private def readMultiPointMRecord: MultiPointRecord = readMultiPointRecord(1)

  private def readMultiPointZRecord: MultiPointRecord = readMultiPointRecord(2)

  private def readMultiLine(forwardStepsMult: Int): MultiLine = {
    moveByteBufferForward(32)

    val multiLine = readMultiLine

    moveByteBufferForward((16 + 8 * multiLine.vertexCount) * forwardStepsMult)

    multiLine
  }

  private def readMultiLineRecord(forwardStepsMult: Int): MultiLineRecord =
    MultiLineRecord(readMultiLine(forwardStepsMult))

  private def readMultiLineRecord: MultiLineRecord = readMultiLineRecord(0)

  private def readMultiLineMRecord: MultiLineRecord = readMultiLineRecord(1)

  private def readMultiLineZRecord: MultiLineRecord = readMultiLineRecord(2)

  private def readPolygonRecord(forwardStepsMult: Int): PolygonRecord = {
    moveByteBufferForward(32)

    val polygon = readPolygon

    moveByteBufferForward((16 + 8 * polygon.vertexCount) * forwardStepsMult)

    PolygonRecord(polygon)
  }

  private def readPolygonRecord: PolygonRecord = readPolygonRecord(0)

  private def readPolygonMRecord: PolygonRecord = readPolygonRecord(1)

  private def readPolygonZRecord: PolygonRecord = readPolygonRecord(2)

  import MultiPolygonPartType._

  private def readMultiPolygonRecord = {
    moveByteBufferForward(32)

    val numParts = byteBuffer.getInt
    val numPoints = byteBuffer.getInt

    val partIndices = Array.ofDim[Int](numParts)

    cfor(0)(_ < numParts, _ + 1) { i =>
      partIndices(i) = byteBuffer.getInt
    }

    val partTypes = Array.ofDim[Int](numParts)

    cfor(0)(_ < numParts, _ + 1) { i =>
      partTypes(i) = byteBuffer.getInt
    }

    val polygons = ListBuffer[Polygon]()

    var idx = 0
    def calcSize(idx: Int) = {
        if (idx != numParts - 1) partIndices(idx + 1) - partIndices(idx)
        else numPoints
    }

    while (idx < numParts) {
      partTypes(idx) match {
        case TriangleStrip | TriangleFan =>
          polygons += Polygon(Line(readPoints(calcSize(idx))))
        case OuterRing => {
          val size = calcSize(idx)

          val outer = Line(readPoints(size))
          var inners = ListBuffer[Line]()

          idx += 1
          while (partTypes(idx) == InnerRing) {
            inners += Line(readPoints(calcSize(idx)))
            idx += 1
          }
          idx -= 1

          polygons += Polygon(outer, inners)
        }
        case FirstRing => {
          val size = calcSize(idx)

          val first = Line(readPoints(size))
          var rings = ListBuffer[Line]()

          idx += 1
          while (partTypes(idx) == Ring) {
            rings += Line(readPoints(calcSize(idx)))
            idx += 1
          }
          idx -= 1

          polygons += Polygon(first, rings)
        }
        case Ring => polygons += Polygon(Line(readPoints(calcSize(idx))))
        case malformedMultiPolygonType =>
          throw new MalformedShapeFileException(
            s"""${malformedMultiPolygonType} is not a valid multi patch type.""")
      }

      idx += 1
    }

    MultiPolygonRecord(MultiPolygon(polygons))
  }

}
