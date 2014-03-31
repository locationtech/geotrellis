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

package geotrellis.benchmark.io

import java.nio.ByteBuffer
import geotrellis._
import geotrellis.data._
import geotrellis.util._
import geotrellis.process._
import geotrellis.util.Filesystem
import geotrellis.raster._

abstract class IntNReadState(data:Either[String, Array[Byte]],
                             val rasterExtent:RasterExtent,
                             val target:RasterExtent,
                             typ:RasterType) extends IntReadState {
  def getType = typ

  final val width:Int = typ.bits / 8

  protected[this] var src:ByteBuffer = null

  /**
   * Returns this datatype's no data value. For signed integers, this is the
   * minimum value storable at this particular bitwidth. For instance, for
   * 8-bit integers (width 1), the value -128 is the no data value.
   */
  def getNoDataValue:Int = -(1 << (width * 8 - 1))

  def initSource(pos:Int, size:Int) {
    src = data match {
      case Left(path) => Filesystem.slurpToBuffer(path, pos * width, size * width)
      case Right(bytes) => ByteBuffer.wrap(bytes, pos * width, size * width)
    }
  }
}

class Int8ReadState(data:Either[String, Array[Byte]],
                    rasterExtent:RasterExtent,
                    target:RasterExtent)
extends IntNReadState(data, rasterExtent, target, TypeByte) {
  @inline final def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    dest(destIndex) = src.get(sourceIndex)
  }
}

class Int16ReadState(data:Either[String, Array[Byte]],
                     rasterExtent:RasterExtent,
                     target:RasterExtent)
extends IntNReadState(data, rasterExtent, target, TypeShort) {
  @inline final def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    dest(destIndex) = src.getShort(sourceIndex * 2)
  }
}

class Int32ReadState(data:Either[String, Array[Byte]],
                     rasterExtent:RasterExtent,
                     target:RasterExtent)
extends IntNReadState(data, rasterExtent, target, TypeInt) {
  @inline final def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    dest(destIndex) = src.getInt(sourceIndex * 4)
  }
}

final class Int1ReadState(data:Either[String, Array[Byte]],
                          val rasterExtent:RasterExtent,
                          val target:RasterExtent) 
      extends ReadState {
  private var src:ByteBuffer = null
  private var remainder:Int = 0

  def getType = TypeBit

  def initSource(pos:Int, size:Int) {
    val p = pos / 8
    val s = (size + 7) / 8
    src = data match {
      case Left(path) => Filesystem.slurpToBuffer(path, p, s)
      case Right(bytes) => ByteBuffer.wrap(bytes, p, s)
    }
  }

  @inline final def assignFromSource(sourceIndex:Int, dest:MutableRasterData, destIndex:Int) {
    val i = (sourceIndex + remainder)
    dest(destIndex) = (src.get(i >> 3) >> (i & 7)) & 1
  }
}
