package geotrellis.raster.io.shape.reader

import geotrellis.raster.io.Filesystem

import geotrellis.vector._

import java.nio.{ByteBuffer, ByteOrder}

import collection.mutable.{ArrayBuffer, ListBuffer}

import spire.syntax.cfor._

case class MalformedShapePointFileException(msg: String) extends RuntimeException(msg)

object ShapePointFileReader {

  def apply(path: String): ShapePointFileReader =
    if (path.endsWith(".shp")) apply(Filesystem.slurp(path))
    else throw new MalformedShapePointFileException("Bad file ending (must be .shp).")

  def apply(bytes: Array[Byte]): ShapePointFileReader =
    new ShapePointFileReader(ByteBuffer.wrap(bytes, 0, bytes.size))

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

class ShapePointFileReader(byteBuffer: ByteBuffer) extends ShapeHeaderReader {

  import ShapeType._

  private val boundingBox = Array.ofDim[Double](8)

  lazy val read: ShapePointFile = {
    val boundingBox = readHeader(byteBuffer)

    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    val recordBuffer = ArrayBuffer[ShapePointRecord]()
    while (byteBuffer.remaining > 0) recordBuffer += readPointRecord

    ShapePointFile(recordBuffer.toArray, boundingBox)
  }

  private def readPointRecord: ShapePointRecord = {
    byteBuffer.order(ByteOrder.BIG_ENDIAN)
    val nr = byteBuffer.getInt
    val size = byteBuffer.getInt

    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    byteBuffer.getInt match {
      case PointType => readPointPointRecord
      case MultiPointType => readMultiPointPointRecord
      case MultiLineType => readMultiLinePointRecord
      case PolygonType => readPolygonPointRecord
      case PointMType => readPointMPointRecord
      case MultiPointMType => readMultiPointMPointRecord
      case MultiLineMType => readMultiLineMPointRecord
      case PolygonMType => readPolygonMPointRecord
      case PointZType => readPointZPointRecord
      case MultiPointZType => readMultiPointZPointRecord
      case MultiLineZType => readMultiLineZPointRecord
      case PolygonZType => readPolygonZPointRecord
      case MultiPolygonType => readMultiPolygonPointRecord
      case malformedShapeType => throw new MalformedShapePointFileException(
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

  private def readPointPointRecord(forwardSteps: Int): PointPointRecord = {
    val res = PointPointRecord(readPoint)

    moveByteBufferForward(forwardSteps)

    res
  }

  private def readPointPointRecord: PointPointRecord = readPointPointRecord(0)

  private def readPointMPointRecord: PointPointRecord = readPointPointRecord(8)

  private def readPointZPointRecord: PointPointRecord = readPointPointRecord(16)

  private def readMultiPointPointRecord(forwardStepsMult: Int): MultiPointPointRecord = {
    moveByteBufferForward(32)

    val multiPoint = readMultiPoint

    moveByteBufferForward((16 + 8 * multiPoint.vertexCount) * forwardStepsMult)

    MultiPointPointRecord(multiPoint)
  }

  private def readMultiPointPointRecord: MultiPointPointRecord = readMultiPointPointRecord(0)

  private def readMultiPointMPointRecord: MultiPointPointRecord = readMultiPointPointRecord(1)

  private def readMultiPointZPointRecord: MultiPointPointRecord = readMultiPointPointRecord(2)

  private def readMultiLine(forwardStepsMult: Int): MultiLine = {
    moveByteBufferForward(32)

    val multiLine = readMultiLine

    moveByteBufferForward((16 + 8 * multiLine.vertexCount) * forwardStepsMult)

    multiLine
  }

  private def readMultiLinePointRecord(forwardStepsMult: Int): MultiLinePointRecord =
    MultiLinePointRecord(readMultiLine(forwardStepsMult))

  private def readMultiLinePointRecord: MultiLinePointRecord = readMultiLinePointRecord(0)

  private def readMultiLineMPointRecord: MultiLinePointRecord = readMultiLinePointRecord(1)

  private def readMultiLineZPointRecord: MultiLinePointRecord = readMultiLinePointRecord(2)

  private def readPolygonPointRecord(forwardStepsMult: Int): PolygonPointRecord = {
    moveByteBufferForward(32)

    val polygon = readPolygon

    moveByteBufferForward((16 + 8 * polygon.vertexCount) * forwardStepsMult)

    PolygonPointRecord(polygon)
  }

  private def readPolygonPointRecord: PolygonPointRecord = readPolygonPointRecord(0)

  private def readPolygonMPointRecord: PolygonPointRecord = readPolygonPointRecord(1)

  private def readPolygonZPointRecord: PolygonPointRecord = readPolygonPointRecord(2)

  import MultiPolygonPartType._

  private def readMultiPolygonPointRecord = {
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
          throw new MalformedShapePointFileException(
            s"""${malformedMultiPolygonType} is not a valid multi patch type.""")
      }

      idx += 1
    }

    MultiPolygonPointRecord(MultiPolygon(polygons))
  }

}
