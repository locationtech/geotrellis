package geotrellis.vector.io.shape.reader

import geotrellis.vector._
import geotrellis.vector.io.FileSystem

import java.nio.{ByteBuffer, ByteOrder}

import collection.mutable.ArrayBuffer

import spire.syntax.cfor._

case class MalformedShapePointFileException(msg: String) extends RuntimeException(msg)

object ShapePointFileReader {

  val FileExtension = ".shp"

  def apply(path: String): ShapePointFileReader =
    if (path.endsWith(FileExtension)) {
      val bytes = 
        FileSystem.slurp(path)
      apply(bytes)
    } else {
      throw new MalformedShapePointFileException(s"Bad file ending (must be $FileExtension).")
    }

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

/** ShapePointFileReader is used to read the actual geometries form the shapefile.
  * It reads points out of sections of the byte buffer based on what type of geometry is being read.
  * 
  * @param      byteBuffer           ByteBuffer containing the data to read.
  * @param      threeDimensional     Flag to indicate that these points have 3 components, (x, y, z). False by default (just x and y points).           
  */
class ShapePointFileReader(byteBuffer: ByteBuffer, threeDimensionsal: Boolean = false) extends ShapeHeaderReader {

  import ShapeType._

  private val boundingBox = Array.ofDim[Double](8)

  lazy val read: ShapePointFile = {
    val boundingBox = 
      readHeader(byteBuffer)

    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    val recordBuffer = ArrayBuffer[ShapePointRecord]()
    while (byteBuffer.remaining > 0) {
      val record =
        readPointRecord
      recordBuffer += record
    }

    ShapePointFile(recordBuffer.toArray, boundingBox)
  }

  @inline
  private final def readPointRecord: ShapePointRecord = {
    byteBuffer.order(ByteOrder.BIG_ENDIAN)
    val nr = byteBuffer.getInt
    val size = byteBuffer.getInt

    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
    byteBuffer.getInt  match {
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

  @inline
  private final def readPoint = 
    Point(byteBuffer.getDouble, byteBuffer.getDouble)

  @inline
  private final def readPoints(): Array[Point] = {
    readPoints(byteBuffer.getInt)
  }

  @inline
  private final def readPoints(numPoints: Int): Array[Point] = {
    val points = Array.ofDim[Point](numPoints)

    cfor(0)(_ < numPoints, _ + 1) { i =>
      points(i) = readPoint
    }

    points
  }

  // @inline
  // private final def readPoints(): Array[Point] = {
  //   readPointArrays(byteBuffer.getInt)
  // }

  // @inline
  // private final def readPointArrays(numPoints: Int): (Array[Double], Array[Double]) = {
  //   val points = Array.ofDim[Point](numPoints)

  //   cfor(0)(_ < numPoints, _ + 1) { i =>
  //     points(i) = readPoint
  //   }

  //   points
  // }

  @inline
  private final def readMultiPoint = MultiPoint(readPoints)

  @inline
  private final def readLines = {
    val numParts = byteBuffer.getInt
    val numPoints = byteBuffer.getInt

    val lines = Array.ofDim[Line](numParts)
    val offsets = Array.ofDim[Int](numParts)

    cfor(0)(_ < numParts, _ + 1) { i =>
      offsets(i) = byteBuffer.getInt
    }

    cfor(0)(_ < numParts, _ + 1) { i =>
      val start = offsets(i)
      val end = if (i == numParts - 1) numPoints else offsets(i + 1)
      lines(i) = Line(readPoints(end - start))
    }

    lines
  }

  @inline
  private final def readMultiLine = MultiLine(readLines)

  // TODO: Add to line class?
  @inline
  private final def isLineClockWise(line: Line) = {
    val points = line.points
    var s = 0.0

    cfor(0)(_ < points.size - 1, _ + 1) { i =>
      val p1 = points(i)
      val p2 = points(i + 1)
      s += (p2.x - p1.x) * (p2.y + p1.y)
    }

    s > 0
  }

/*
    public Object read(ByteBuffer buffer, ShapeType type, boolean flatFeature) {
        if (type == ShapeType.NULL) {
            return createNull();
        }
        // bounds
        buffer.position(buffer.position() + 4 * 8);

        int[] partOffsets;

        int numParts = buffer.getInt();
        int numPoints = buffer.getInt();
        int dimensions = (shapeType == ShapeType.POLYGONZ) && !flatFeature ? 3 : 2;

        partOffsets = new int[numParts];

        for (int i = 0; i < numParts; i++) {
            partOffsets[i] = buffer.getInt();
        }

        ArrayList shells = new ArrayList();
        ArrayList holes = new ArrayList();
        CoordinateSequence coords = readCoordinates(buffer, numPoints, dimensions);

        int offset = 0;
        int start;
        int finish;
        int length;

        for (int part = 0; part < numParts; part++) {
            start = partOffsets[part];

            if (part == (numParts - 1)) {
                finish = numPoints;
            } else {
                finish = partOffsets[part + 1];
            }

            length = finish - start;
            int close = 0; // '1' if the ring must be closed, '0' otherwise
            if ((coords.getOrdinate(start, 0) != coords.getOrdinate(finish - 1, 0)) 
                    || (coords.getOrdinate(start, 1) != coords.getOrdinate(finish - 1, 1))
            ) {
                close=1;
            }
            if (dimensions == 3) {
                if(coords.getOrdinate(start, 2) != coords.getOrdinate(finish - 1, 2)) {
                    close = 1;
                }
            }

            CoordinateSequence csRing = geometryFactory.getCoordinateSequenceFactory().create(length + close, dimensions);
            // double area = 0;
            // int sx = offset;
            for (int i = 0; i < length; i++) {
                csRing.setOrdinate(i, 0, coords.getOrdinate(offset, 0));
                csRing.setOrdinate(i, 1, coords.getOrdinate(offset, 1));
                if(dimensions == 3) {
                    csRing.setOrdinate(i, 2, coords.getOrdinate(offset, 2));
                }
                offset++;
            }
            if (close == 1) {
                csRing.setOrdinate(length, 0, coords.getOrdinate(start, 0));
                csRing.setOrdinate(length, 1, coords.getOrdinate(start, 1));
                if(dimensions == 3) {
                    csRing.setOrdinate(length, 2, coords.getOrdinate(start, 2));
                }
            }
            // REVISIT: polygons with only 1 or 2 points are not polygons -
            // geometryFactory will bomb so we skip if we find one.
            if (csRing.size() == 0 || csRing.size() > 3) {
                LinearRing ring = geometryFactory.createLinearRing(csRing);

                if (CoordinateSequences.isCCW(csRing)) {
                    // counter-clockwise
                    holes.add(ring);
                } else {
                    // clockwise
                    shells.add(ring);
                }
            }
        }

        // quick optimization: if there's only one shell no need to check
        // for holes inclusion
        if (shells.size() == 1) {
            return createMulti((LinearRing) shells.get(0), holes);
        }
        // if for some reason, there is only one hole, we just reverse it and
        // carry on.
        else if (holes.size() == 1 && shells.size() == 0) {
            return createMulti((LinearRing) holes.get(0));
        } else {

            // build an association between shells and holes
            final ArrayList holesForShells = assignHolesToShells(shells, holes);

            Geometry g = buildGeometries(shells, holes, holesForShells);

            return g;
        }
    }
 */
  @inline
  private final def readPolygon = {
    val lines = {
      val numParts = byteBuffer.getInt
      val numPoints = byteBuffer.getInt

      // TODO: Determine dimensions

      val lines = Array.ofDim[Line](numParts)
      val offsets = Array.ofDim[Int](numParts)

      cfor(0)(_ < numParts, _ + 1) { i =>
        offsets(i) = byteBuffer.getInt
      }

      cfor(0)(_ < numParts, _ + 1) { i =>
        val start = offsets(i)
        val end = if (i == numParts - 1) numPoints else offsets(i + 1)
        lines(i) = Line(readPoints(end - start))
      }

      lines
    }

    val used = Array.ofDim[Boolean](lines.size)
    val outersBuffer = ArrayBuffer[Polygon]()
    val innersBuffer = ArrayBuffer[Line]()


    cfor(0)(_ < lines.size, _ + 1) { i =>
      val line = lines(i)
      if (isLineClockWise(line)) outersBuffer += Polygon(line)
      else innersBuffer += line
    }

    val outers = outersBuffer.toArray
    val inners = innersBuffer.toArray
    val holes = Array.ofDim[ArrayBuffer[Line]](outers.size)
    cfor(0)(_ < holes.size, _ + 1) { i =>
      holes(i) = ArrayBuffer[Line]()
    }

    cfor(0)(_ < inners.size, _ + 1) { i =>
      val innerLine = inners(i)
      var found = false
      cfor(0)(_ < outers.size && !found, _ + 1) { j =>
        if (outers(j).contains(innerLine)) {
          holes(j) += innerLine
          found = true
        }
      }
    }

    val polygons = Array.ofDim[Polygon](outers.size)
    cfor(0)(_ < polygons.size, _ + 1) { i =>
      polygons(i) =
        if (!holes(i).isEmpty) Polygon(outers(i).exterior, holes(i).toArray)
        else outers(i)
    }

    MultiPolygon(polygons)
  }

  @inline
  private final def moveByteBufferForward(steps: Int) =
    byteBuffer.position(byteBuffer.position + steps)

  @inline
  private final def readPointPointRecord(forwardSteps: Int): PointPointRecord = {
    val res = PointPointRecord(readPoint)

    moveByteBufferForward(forwardSteps)

    res
  }

  @inline
  private final def readPointPointRecord: PointPointRecord = readPointPointRecord(0)

  @inline
  private final def readPointMPointRecord: PointPointRecord = readPointPointRecord(8)

  @inline
  private final def readPointZPointRecord: PointPointRecord = readPointPointRecord(16)

  @inline
  private final def readMultiPointPointRecord(forwardStepsMult: Int): MultiPointPointRecord = {
    moveByteBufferForward(32)

    val multiPoint = readMultiPoint

    moveByteBufferForward((16 + 8 * multiPoint.vertexCount) * forwardStepsMult)

    MultiPointPointRecord(multiPoint)
  }

  @inline
  private final def readMultiPointPointRecord: MultiPointPointRecord =
    readMultiPointPointRecord(0)

  @inline
  private final def readMultiPointMPointRecord: MultiPointPointRecord =
    readMultiPointPointRecord(1)

  @inline
  private final def readMultiPointZPointRecord: MultiPointPointRecord =
    readMultiPointPointRecord(2)

  @inline
  private final def readMultiLine(forwardStepsMult: Int): MultiLine = {
    moveByteBufferForward(32)

    val multiLine = readMultiLine

    moveByteBufferForward((16 + 8 * multiLine.vertexCount) * forwardStepsMult)

    multiLine
  }

  @inline
  private final def readMultiLinePointRecord(forwardStepsMult: Int): MultiLinePointRecord =
    MultiLinePointRecord(readMultiLine(forwardStepsMult))

  @inline
  private final def readMultiLinePointRecord: MultiLinePointRecord =
    readMultiLinePointRecord(0)

  @inline
  private final def readMultiLineMPointRecord: MultiLinePointRecord =
    readMultiLinePointRecord(1)

  @inline
  private final def readMultiLineZPointRecord: MultiLinePointRecord =
    readMultiLinePointRecord(2)

  @inline
  private final def readPolygonPointRecord(forwardStepsMult: Int): MultiPolygonPointRecord = {
    moveByteBufferForward(32)

    val multiPolygon = 
//      readPolygon
      MultiPolygonReader.read(byteBuffer)

    moveByteBufferForward((16 + 8 * multiPolygon.vertexCount) * forwardStepsMult)


    MultiPolygonPointRecord(multiPolygon)
  }

  @inline
  private final def readPolygonPointRecord: MultiPolygonPointRecord =
    readPolygonPointRecord(0)

  @inline
  private final def readPolygonMPointRecord: MultiPolygonPointRecord =
    readPolygonPointRecord(1)

  @inline
  private final def readPolygonZPointRecord: MultiPolygonPointRecord =
    readPolygonPointRecord(2)

  import MultiPolygonPartType._

  @inline
  private final def readMultiPolygonPointRecord = {
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

    val polygons = ArrayBuffer[Polygon]()

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
          var inners = ArrayBuffer[Line]()

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
          var rings = ArrayBuffer[Line]()

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
