/***
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
 ***/

package geotrellis

import java.awt.image.DataBuffer

sealed abstract class RasterType(val precedence: Int, val float: Boolean, val name: String) extends Serializable {
  def bits = if (float) precedence / 10 else precedence
  def bytes = bits / 8
  def union(rhs: RasterType) = if (precedence < rhs.precedence) rhs else this
  def intersect(rhs: RasterType) = if (precedence < rhs.precedence) this else rhs

  def contains(rhs: RasterType) = precedence >= rhs.precedence

  def isDouble = precedence > 32

  def numBytes(size: Int) = bytes * size

}

case object TypeBit extends RasterType(1, false, "bool") {
  override final def numBytes(size: Int) = (size + 7) / 8
}

case object TypeByte extends RasterType(8, false, "int8")
case object TypeShort extends RasterType(16, false, "int16")
case object TypeInt extends RasterType(32, false, "int32")
case object TypeFloat extends RasterType(320, true, "float32")
case object TypeDouble extends RasterType(640, true, "float64")

object RasterType {
  def fromAwtType(awtType: Int): RasterType = awtType match {
    case DataBuffer.TYPE_BYTE   => TypeByte
    case DataBuffer.TYPE_DOUBLE => TypeDouble
    case DataBuffer.TYPE_FLOAT  => TypeFloat
    case DataBuffer.TYPE_INT    => TypeInt
    case DataBuffer.TYPE_SHORT  => TypeShort
    case _                      => sys.error(s"Oops type $awtType is not supported")
  }

  def toAwtType(rasterType: RasterType): Int = rasterType match {
    case TypeBit | TypeByte => DataBuffer.TYPE_BYTE
    case TypeDouble         => DataBuffer.TYPE_DOUBLE
    case TypeFloat          => DataBuffer.TYPE_FLOAT
    case TypeInt            => DataBuffer.TYPE_INT
    case TypeShort          => DataBuffer.TYPE_SHORT
  }
}