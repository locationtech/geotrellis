package geotrellis.vector.io.shape.reader

import geotrellis.vector._

import java.nio.ByteBuffer

object GeometryReader {

  final val PointType = 1
  final val MultiLineType = 3
  final val PolygonType = 5
  final val MultiPointType = 8
  final val PointZType = 11
  final val MultiLineZType = 13
  final val PolygonZType = 15
  final val MultiPointZType = 18
  final val PointMType = 21
  final val MultiLineMType = 23
  final val PolygonMType = 25
  final val MultiPointMType = 28
  final val MultiPolygonType = 31


  def apply(typeCode: Int): GeometryReader =
    typeCode match {
      case PointType => new PointReader(2)
      case PointMType => new PointReader(3)
      case PointZType => new PointReader(3)
      case PolygonType => new MultiPolygonReader(2)
      case PolygonMType => new MultiPolygonReader(3)
      case PolygonZType => new MultiPolygonReader(3)
      case MultiPointType => new MultiPointReader(2)
      case MultiPointMType => new MultiPointReader(3)
      case MultiPointZType => new MultiPointReader(3)
      case MultiLineType => new MultiLineReader(2)
      case MultiLineMType => new MultiLineReader(3)
      case MultiLineZType => new MultiLineReader(3)
      case MultiPolygonType => ???
      case malformedShapeType => 
        throw new MalformedShapePointFileException(
          s"""Malformed shape type: $malformedShapeType."""
        )
    }
}

abstract class GeometryReader(dimensions: Int, hasBoundingBox: Boolean = true) {
  def apply(byteBuffer: ByteBuffer): Geometry = {
    if(hasBoundingBox) { byteBuffer.moveForward(32) }
    val geom = readGeometry(byteBuffer)
    if(dimensions > 2) {
      // Skip past Z values
      byteBuffer.moveForward((geom.vertexCount * 8) + 16)
    }
    geom
  }

  def readGeometry(byteBuffer: ByteBuffer): Geometry
}


/*****What's going on with the MultiPolygonType reader? */
// object MultiPolygonPartType {

//   val TriangleStrip = 0
//   val TriangleFan = 1
//   val OuterRing = 2
//   val InnerRing = 3
//   val FirstRing = 4
//   val Ring = 5

// }


  // import MultiPolygonPartType._

  // @inline
  // private final def readMultiPolygonPointRecord = {
  //   moveByteBufferForward(32)

  //   val numParts = byteBuffer.getInt
  //   val numPoints = byteBuffer.getInt

  //   val partIndices = Array.ofDim[Int](numParts)

  //   cfor(0)(_ < numParts, _ + 1) { i =>
  //     partIndices(i) = byteBuffer.getInt
  //   }

  //   val partTypes = Array.ofDim[Int](numParts)

  //   cfor(0)(_ < numParts, _ + 1) { i =>
  //     partTypes(i) = byteBuffer.getInt
  //   }

  //   val polygons = ArrayBuffer[Polygon]()

  //   var idx = 0
  //   def calcSize(idx: Int) = {
  //     if (idx != numParts - 1) partIndices(idx + 1) - partIndices(idx)
  //     else numPoints
  //   }

  //   while (idx < numParts) {
  //     partTypes(idx) match {
  //       case TriangleStrip | TriangleFan =>
  //         polygons += Polygon(Line(readPoints(calcSize(idx))))
  //       case OuterRing => {
  //         val size = calcSize(idx)

  //         val outer = Line(readPoints(size))
  //         var inners = ArrayBuffer[Line]()

  //         idx += 1
  //         while (partTypes(idx) == InnerRing) {
  //           inners += Line(readPoints(calcSize(idx)))
  //           idx += 1
  //         }
  //         idx -= 1

  //         polygons += Polygon(outer, inners)
  //       }
  //       case FirstRing => {
  //         val size = calcSize(idx)

  //         val first = Line(readPoints(size))
  //         var rings = ArrayBuffer[Line]()

  //         idx += 1
  //         while (partTypes(idx) == Ring) {
  //           rings += Line(readPoints(calcSize(idx)))
  //           idx += 1
  //         }
  //         idx -= 1

  //         polygons += Polygon(first, rings)
  //       }
  //       case Ring => polygons += Polygon(Line(readPoints(calcSize(idx))))
  //       case malformedMultiPolygonType =>
  //         throw new MalformedShapePointFileException(
  //           s"""${malformedMultiPolygonType} is not a valid multi patch type.""")
  //     }

  //     idx += 1
  //   }

  //   MultiPolygonPointRecord(MultiPolygon(polygons))
  // }
