/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io._
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

import geotrellis.raster.io.geotiff.tags.codes._
import scala.collection.mutable

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteOrder

import spire.syntax.cfor._

object GeoTiffWriter {
  def write(geoTiff: GeoTiff, path: String): Unit = {
    val fos = new FileOutputStream(new File(path))
    try {
      val dos = new DataOutputStream(fos)
      try {
        new GeoTiffWriter(geoTiff, dos).write()
      } finally {
        dos.close
      }
    } finally {
      fos.close
    }
  }
}

class GeoTiffWriter(geoTiff: GeoTiff, dos: DataOutputStream) {
  implicit val toBytes: ToBytes =
    if(geoTiff.imageData.decompressor.byteOrder == ByteOrder.BIG_ENDIAN)
      BigEndianToBytes
    else
      LittleEndianToBytes

  val (fieldValues, offsetFieldValueBuilder) = TiffTagFieldValue.collect(geoTiff)
  val segments = geoTiff.imageData.compressedBytes
  val segmentCount = segments.size

  val tagFieldByteCount = (fieldValues.length + 1) * 12 // Tiff Tag Fields are 12 bytes long.

  val tagDataByteCount = {
    var s = 0
    cfor(0)(_ < fieldValues.length, _ + 1) { i =>
      val len = fieldValues(i).value.length
      // If value fits in 4 bytes, we store it in the offset,
      // so only count data more than 4 bytes.
      if(len > 4) {
        s += fieldValues(i).value.length
      }
    }

    // Account for offsetFieldValue size
    // each offset is an Int long.
    if(segmentCount > 1) {
      s += (segmentCount * 4)
    }

    s
  }

  // Used to immediately write values to our ultimate destination.
  var index: Int = 0
  def writeByte(value: Byte) { dos.writeByte(value); index += 1 }
  def writeBytes(value: Array[Byte]) { dos.write(value, 0, value.length); index += value.length }

  def writeShort(value: Int) { writeBytes(toBytes(value.toShort)) }
  def writeInt(value: Int) { writeBytes(toBytes(value)) }
  def writeLong(value: Long) { writeBytes(toBytes(value)) }
  def writeFloat(value: Float) { writeBytes(toBytes(value)) }
  def writeDouble(value: Double) { writeBytes(toBytes(value)) }

  def write(): Unit = {

    // Write the header that determines the endian
    if(geoTiff.imageData.decompressor.byteOrder == ByteOrder.BIG_ENDIAN) {
      val m = 'M'.toByte
      writeByte(m)
      writeByte(m)
    } else {
      val i = 'I'.toByte
      writeByte(i)
      writeByte(i)
    }

    // TIFF header code.
    writeShort(42.toShort)

    // Write tag start offset (immediately after this 4 byte integer)
    writeInt(index + 4)

    // Compute the offsetFieldValue
    val offsetFieldValue = {
      val imageDataStartOffset =
        index +
          2 + // Short for number of tags
          4 + // Int for next IFD address
          tagFieldByteCount + tagDataByteCount


      val offsets = Array.ofDim[Int](segmentCount)
      var offset = imageDataStartOffset
      cfor(0)(_ < segmentCount, _ + 1) { i =>
        offsets(i) = offset
        offset += segments(i).length
      }
      offsetFieldValueBuilder(offsets)
    }

    // Sort the fields by tag code.
    val sortedTagFieldValues = (offsetFieldValue :: fieldValues.toList).sortBy(_.tag).toArray

    // Write the number of tags
    writeShort(sortedTagFieldValues.length)

    // Write tag fields, sorted by tag code.
    val tagDataStartOffset = 
      index + 
        4 + // Int for next IFD address
        tagFieldByteCount

    var tagDataOffset = tagDataStartOffset

    cfor(0)(_ < sortedTagFieldValues.length, _ + 1) { i =>
      val TiffTagFieldValue(tag, fieldType, length, value) = sortedTagFieldValues(i)
      writeShort(tag)
      writeShort(fieldType)
      writeInt(length)
      if(value.length > 4) {
        writeInt(tagDataOffset)
        tagDataOffset += value.length
      } else {
        var i = 0
        while(i < value.length) {
          writeByte(value(i))
          i += 1
        }
        while(i < 4) {
          writeByte(0.toByte)
          i += 1
        }
      }
    }

    // Write 0 integer to indicate the end of the last IFD.
    writeInt(0)

    assert(index == tagDataStartOffset, s"Writer error: index at $index, should be $tagDataStartOffset")
    assert(tagDataOffset == tagDataStartOffset + tagDataByteCount)

    // write tag data
    cfor(0)(_ < sortedTagFieldValues.length, _ + 1) { i =>
      val TiffTagFieldValue(tag, fieldType, length, value) = sortedTagFieldValues(i)
      if(value.length > 4) {
        writeBytes(value)
      }
    }

    // Write the image data.
    cfor(0)(_ < segmentCount, _ + 1) { i =>
      writeBytes(segments(i))
    }

    dos.flush()
  }
}
