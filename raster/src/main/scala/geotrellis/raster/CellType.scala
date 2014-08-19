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

package geotrellis.raster

import java.awt.image.DataBuffer

sealed abstract class CellType(val bits: Int, val name: String, val isFloatingPoint: Boolean) extends Serializable {
  def bytes = bits / 8
  def union(other: CellType) = 
    if (bits < other.bits) 
      other
    else if (bits < other.bits)
      this
    else if (isFloatingPoint && !other.isFloatingPoint)
      this
    else
      other

  def intersect(other: CellType) = 
    if (bits < other.bits)
      this
    else if (bits < other.bits)
      other
    else if (isFloatingPoint && !other.isFloatingPoint)
      other
    else
      this

  def contains(other: CellType) = bits >= other.bits

  def numBytes(size: Int) = bytes * size

  override def toString: String = name
}

case object TypeBit extends CellType(1, "bool", false) {
  override final def numBytes(size: Int) = (size + 7) / 8
}

case object TypeByte extends CellType(8, "int8", false)
case object TypeShort extends CellType(16, "int16", false)
case object TypeInt extends CellType(32, "int32", false)
case object TypeFloat extends CellType(32, "float32", true)
case object TypeDouble extends CellType(64, "float64", true)

object CellType {
  def fromAwtType(awtType: Int): CellType = awtType match {
    case DataBuffer.TYPE_BYTE   => TypeByte
    case DataBuffer.TYPE_DOUBLE => TypeDouble
    case DataBuffer.TYPE_FLOAT  => TypeFloat
    case DataBuffer.TYPE_INT    => TypeInt
    case DataBuffer.TYPE_SHORT  => TypeShort
    case _                      => sys.error(s"Cell type with AWT type $awtType is not supported")
  }

  def fromString(name: String): CellType = name match {
    case "int8"     => TypeByte
    case "int16"    => TypeShort
    case "int32"    => TypeInt
    case "float32"  => TypeFloat
    case "float64"  => TypeDouble
    case _ => sys.error(s"Cell type $name is not supported")
  }

  def toAwtType(cellType: CellType): Int = cellType match {
    case TypeBit | TypeByte => DataBuffer.TYPE_BYTE
    case TypeDouble         => DataBuffer.TYPE_DOUBLE
    case TypeFloat          => DataBuffer.TYPE_FLOAT
    case TypeInt            => DataBuffer.TYPE_INT
    case TypeShort          => DataBuffer.TYPE_SHORT
  }
}
