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

package geotrellis.io.geotiff.decompression

import java.nio.{ByteBuffer, ByteOrder}

import monocle.syntax._
import monocle.Macro._

import geotrellis.io.geotiff._
import geotrellis.io.geotiff.ImageDirectoryLenses._

object JpegTags {

  val SOI = 0xd8
  val SOF0 = 0xc0
  val DHT = 0xc4
  val DQT = 0xdb
  val DRI = 0xdd
  val SOS = 0xDA
  val RST0 = 0xd0
  val RST1 = 0xd1
  val RST2 = 0xd2
  val RST3 = 0xd3
  val RST4 = 0xd4
  val RST5 = 0xd5
  val RST6 = 0xd6
  val RST7 = 0xd7
  val COM = 0xfe
  val EOI = 0xD9
}

case class JpegHeader(
  isBaseLineDCT: Option[Boolean] = None,
  isProgressiveDCT: Option[Boolean] = None)

object JpegHeaderLenses {

}

case class JpegTables()

object JpegDecompression {

  implicit class Jpeg(matrix: Vector[Vector[Byte]]) {

    import JpegTags._

    def uncompressJpeg(directory: ImageDirectory): Vector[Byte] = {
      val jpegTables = directory |-> jpegTablesLens get match {
        case Some(v) => Some(JpegTables())
        case None => None
      }

      matrix.map(uncompressJpegSegment(_, directory,
        jpegTables)).flatten.toVector
    }

    private def uncompressJpegSegment(segment: Vector[Byte],
      directory: ImageDirectory, jpegTables: Option[JpegTables]) = {
      val byteBuffer = ByteBuffer.wrap(segment.toArray).
        order(ByteOrder.BIG_ENDIAN)
      validateHeader(byteBuffer)


      segment
    }

    private def readJpegHeader(byteBuffer: ByteBuffer,
      header: JpegHeader): JpegHeader = {
      byteBuffer.getShort match {
        case SOF0 => header
        case DHT => header
        case DQT => header
        case DRI => header
        case SOS => header
        case RST0 => header
        case RST1 => header
        case RST2 => header
        case RST3 => header
        case RST4 => header
        case RST5 => header
        case RST6 => header
        case RST7 => header
        case COM => header
        case EOI => header
      }
    }

    private def validateHeader(byteBuffer: ByteBuffer) =
      if (byteBuffer.getShort != SOI)
        throw new MalformedGeoTiffException("no jpeg SOI")

  }

}
