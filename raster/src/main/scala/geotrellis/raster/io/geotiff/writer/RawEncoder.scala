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

package geotrellis.raster.io.geotiff.writer

import geotrellis.raster._

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

import scala.collection.mutable
import scala.annotation.switch

import scala.math.{ceil, min}

object RawEncoder {
  def render(encoder: Encoder) = 
    RawEncoder(encoder).render

  def apply(encoder: Encoder) = 
    encoder.settings match {
      case Settings(IntSample, Floating, _, _) => new RawFloatEncoder(encoder)
      case Settings(LongSample, Floating, _, _) => new RawDoubleEncoder(encoder)
      case Settings(ByteSample, _, _, _) => new RawByteEncoder(encoder)
      case Settings(ShortSample, _, _, _) => new RawShortEncoder(encoder)
      case Settings(IntSample, _, _, _) => new RawIntEncoder(encoder)
      case s => sys.error(s"can't encoder $s")
    }
}

class RawByteEncoder(encoder: Encoder) extends RawEncoder(encoder) {
  def handleCell(col: Int, row: Int) {
    var z = i2b(tile.get(col, row))
    if (isNoData(z)) z = encoder.settings.nodataInt.toByte
    dmg.writeByte(z)
  }
}

class RawShortEncoder(encoder: Encoder) extends RawEncoder(encoder) {
  def handleCell(col: Int, row: Int) {
    var z = i2s(tile.get(col, row))
    if (isNoData(z)) z = encoder.settings.nodataInt.toShort
    dmg.writeShort(z)
  }
}

class RawIntEncoder(encoder: Encoder) extends RawEncoder(encoder) {
  def handleCell(col: Int, row: Int) {
    var z = tile.get(col, row)
    if (isNoData(z)) z = encoder.settings.nodataInt
    dmg.writeInt(z)
  }
}

class RawFloatEncoder(encoder: Encoder) extends RawEncoder(encoder) {
  val ndf = Float.MinValue
  def handleCell(col: Int, row: Int) {
    var z = d2f(tile.getDouble(col, row))
    dmg.writeFloat(z)
  }
}

class RawDoubleEncoder(encoder: Encoder) extends RawEncoder(encoder) {
  val ndf = Double.MinValue
  def handleCell(col: Int, row: Int) {
    var z = tile.getDouble(col, row)
    dmg.writeDouble(z)
  }
}

abstract class RawEncoder(encoder: Encoder) {
  val cols = encoder.cols
  val rows = encoder.rows
  val tile = encoder.tile
  val dmg = new DataOutputStream(encoder.img)

  def handleCell(col: Int, row: Int): Unit

  def render: (Array[Int], Array[Int]) = {
    var row = 0

    while (row < rows) {
      var col = 0
      while (col < cols) {
        handleCell(col, row)
        col += 1
      }
      row += 1
    }

    val offsets = Array.ofDim[Int](encoder.numStrips)
    val lengths = Array.ofDim[Int](encoder.numStrips)

    if (encoder.numStrips == 1) {
      offsets(0) = encoder.imageStartOffset
      lengths(0) = encoder.bytesPerStrip
    } else {
      val last = encoder.numStrips - 1
      for (i <- 0 until last) {
        offsets(i) = encoder.imageStartOffset + i * encoder.bytesPerStrip
        lengths(i) = encoder.bytesPerStrip
      }
      offsets(last) = encoder.imageStartOffset + last * encoder.bytesPerStrip
      lengths(last) = encoder.bytesPerRow * encoder.leftOverRows
    }

    (offsets, lengths)
  }
}
