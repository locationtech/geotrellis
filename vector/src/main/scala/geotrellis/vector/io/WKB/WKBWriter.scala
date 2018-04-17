/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.vector.io.wkb

import java.io.ByteArrayOutputStream
import java.util.Locale
import org.locationtech.jts.io.{ByteOrderValues, OutStream, OutputStreamOutStream}
import geotrellis.vector._
import org.locationtech.jts.{geom => jts}
import org.locationtech.jts.geom.{Coordinate, CoordinateSequence}

/** Constant values used by the WKB format */
object WKBConstants {
  val wkbXDR: Byte = 0
  val wkbNDR: Byte = 1

  val wkbPoint: Byte = 1
  val wkbLineString: Byte = 2
  val wkbPolygon: Byte = 3
  val wkbMultiPoint: Byte = 4
  val wkbMultiLineString: Byte = 5
  val wkbMultiPolygon: Byte = 6
  val wkbGeometryCollection: Byte = 7
}

/** Companion object to [[WKBWriter]] */
object WKBWriter {

  /** Convert a byte array to a hexadecimal string.
    *
    * @param bytes a byte array
    * @return a string of hexadecimal digits
    */
  def toHex(bytes: Array[Byte]): String  =  bytes.map(b => "%02x".formatLocal(Locale.ENGLISH, b)).mkString
}

/** Ported from JTS WKBWriter [[package org.locationtech.jts.io.WKBWriter]]
  *
  * @author Martin Davis
  */
class WKBWriter(outputDimension: Int, byteOrder: Int) {
  require(outputDimension == 2 || outputDimension == 3, s"Output dimension ($outputDimension) must be 2 or 3")
  require(byteOrder == ByteOrderValues.BIG_ENDIAN || byteOrder == ByteOrderValues.LITTLE_ENDIAN, "Invalid byteOrder")

  def this(outputDimension: Int) = this(outputDimension, ByteOrderValues.BIG_ENDIAN)

  def this() = this(2, ByteOrderValues.BIG_ENDIAN)


  private val  byteArrayOS = new ByteArrayOutputStream()
  // holds output data values
  private val buf = new Array[Byte](8)
  private val byteArrayOutStream = new OutputStreamOutStream(byteArrayOS)
  private var srid: Option[Int] = None

  /** Writes a [[Geometry]] into a byte array.
    *
    * @param geom the geometry to write
    * @return the byte array containing the WKB
    */
  def write(geom: Geometry, srid: Option[Int] = None): Array[Byte] = {
    byteArrayOS.reset()
    this.srid = srid
    write(geom.jtsGeom, byteArrayOutStream)
    byteArrayOS.toByteArray
  }

  /** Writes a [[Geometry]] to an [[OutStream]]}.
    *
    * @param geom the geometry to write
    * @param os the out stream to write to
    * @throws IOException if an I/O error occurs
    */
  private def write(geom: jts.Geometry, os: OutStream) {
    geom match {
      case g: jts.Point => writePoint(g, os)
      case g: jts.LineString => writeLineString(g, os)
      case g: jts.Polygon => writePolygon(g, os)
      case g: jts.MultiPoint => writeGeometryCollection(WKBConstants.wkbMultiPoint, g, os)
      case g: jts.MultiLineString => writeGeometryCollection(WKBConstants.wkbMultiLineString, g, os)
      case g: jts.MultiPolygon => writeGeometryCollection(WKBConstants.wkbMultiPolygon, g, os)
      case g: jts.GeometryCollection => write(g, os)
      case _ => sys.error("Unknown Geometry type")
    }
  }

  private def writePoint(pt: Point, os: OutStream) {
    if (pt.jtsGeom.getCoordinateSequence.size() == 0)
      throw new IllegalArgumentException("Empty Points cannot be represented in WKB")
    writeByteOrder(os)
    writeGeometryType(WKBConstants.wkbPoint, pt, os)
    writeCoordinateSequence(pt.jtsGeom.getCoordinateSequence, false, os)
  }

  private def writeLineString(line: Line, os: OutStream) {
    writeByteOrder(os)
    writeGeometryType(WKBConstants.wkbLineString, line, os)
    writeCoordinateSequence(line.jtsGeom.getCoordinateSequence, true, os)
  }

  private def writePolygon(poly: Polygon, os: OutStream)
  {
    writeByteOrder(os)
    writeGeometryType(WKBConstants.wkbPolygon, poly, os)
    writeInt(poly.jtsGeom.getNumInteriorRing + 1, os)
    writeCoordinateSequence(poly.jtsGeom.getExteriorRing.getCoordinateSequence, true, os)
    for (i <- 0 until poly.jtsGeom.getNumInteriorRing){
      writeCoordinateSequence(poly.jtsGeom.getInteriorRingN(i).getCoordinateSequence, true, os)
    }
  }

  private def writeGeometryCollection(geometryType: Int, gc: GeometryCollection, os: OutStream) {
    writeByteOrder(os)
    writeGeometryType(geometryType, gc, os)
    writeInt(gc.jtsGeom.getNumGeometries, os)
    for (i <- 0 until gc.jtsGeom.getNumGeometries)
      write(gc.jtsGeom.getGeometryN(i), os)
  }

  private def writeByteOrder(os: OutStream) {
    if (byteOrder == ByteOrderValues.LITTLE_ENDIAN)
      buf(0) = WKBConstants.wkbNDR
    else
      buf(0) = WKBConstants.wkbXDR
    os.write(buf, 1)
  }

  private def writeGeometryType(geometryType: Int, g: Geometry, os: OutStream ) {
    val flag3D = if (outputDimension == 3)  0x80000000 else 0
    srid match {
      case Some(srid: Int) =>
        val typeInt = geometryType | flag3D | 0x20000000
        writeInt(typeInt, os)
        writeInt(srid, os)
      case None =>
        val typeInt = geometryType | flag3D
        writeInt(typeInt, os)
    }
  }

  private def writeInt(intValue: Int, os: OutStream) {
    ByteOrderValues.putInt(intValue, buf, byteOrder)
    os.write(buf, 4)
  }

  private def writeCoordinate(seq: CoordinateSequence, index: Int, os: OutStream) {
    ByteOrderValues.putDouble(seq.getX(index), buf, byteOrder)
    os.write(buf, 8)
    ByteOrderValues.putDouble(seq.getY(index), buf, byteOrder)
    os.write(buf, 8)

    // only write 3rd dim if caller has requested it for this writer
    if (outputDimension >= 3) {
      // if 3rd dim is requested, only write it if the CoordinateSequence provides it
      var ordVal = Coordinate.NULL_ORDINATE
      if (seq.getDimension >= 3)
        ordVal = seq.getOrdinate(index, 2)
      ByteOrderValues.putDouble(ordVal, buf, byteOrder)
      os.write(buf, 8)
    }
  }

  private def writeCoordinateSequence(seq: CoordinateSequence, writeSize: Boolean, os: OutStream) {
    if (writeSize) writeInt(seq.size(), os)
    for (i <- 0 until seq.size()) writeCoordinate(seq, i, os)
  }
}
