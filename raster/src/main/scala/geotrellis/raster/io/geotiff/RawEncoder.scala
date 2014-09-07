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

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

import scala.collection.mutable
import scala.annotation.switch

import scala.math.{ceil, min}

object RawEncoder {
  def render(encoder: Encoder) = RawEncoder(encoder).render

  def apply(encoder: Encoder) = encoder.settings match {
    case Settings(IntSample, Floating, _, _) => new RawFloatEncoder(encoder)
    case Settings(LongSample, Floating, _, _) => new RawDoubleEncoder(encoder)
    case Settings(ByteSample, _, _, _) => new RawByteEncoder(encoder)
    case Settings(ShortSample, _, _, _) => new RawShortEncoder(encoder)
    case Settings(IntSample, _, _, _) => new RawIntEncoder(encoder)
    case s => sys.error(s"can't encoder $s")
  }
}

class RawByteEncoder(encoder: Encoder) extends RawEncoder(encoder) {
  def handleCell(i: Int) {
    var z = data.apply(i)
    if (isNoData(z)) z = encoder.settings.nodataInt
    dmg.writeByte(z)
  }
}

class RawShortEncoder(encoder: Encoder) extends RawEncoder(encoder) {
  def handleCell(i: Int) {
    var z = data.apply(i)
    if (isNoData(z)) z = encoder.settings.nodataInt
    dmg.writeShort(z)
  }
}

class RawIntEncoder(encoder: Encoder) extends RawEncoder(encoder) {
  def handleCell(i: Int) {
    var z = data.apply(i)
    if (isNoData(z)) z = encoder.settings.nodataInt
    dmg.writeInt(z)
  }
}

class RawFloatEncoder(encoder: Encoder) extends RawEncoder(encoder) {
  val ndf = Float.MinValue
  def handleCell(i: Int) {
    var z = i2d(data(i))
    dmg.writeFloat(if (isNoData(z)) ndf else z.toFloat)
  }
}

class RawDoubleEncoder(encoder: Encoder) extends RawEncoder(encoder) {
  val ndf = Double.MinValue
  def handleCell(i: Int) {
    var z = i2d(data(i))
    dmg.writeDouble(if (isNoData(z)) ndf else z)
  }
}

abstract class RawEncoder(encoder: Encoder) {
  val cols = encoder.cols
  val rows = encoder.rows
  val data = encoder.data
  val dmg = new DataOutputStream(encoder.img)

  def handleCell(i: Int): Unit

  def render: (Array[Int], Array[Int]) = {
    var row = 0

    while (row < rows) {
      val rowspan = row * cols
      var col = 0
      while (col < cols) {
        handleCell(rowspan + col)
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
