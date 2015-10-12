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

import geotrellis._
import geotrellis.vector.Extent

import java.nio.ByteBuffer

import spire.syntax.cfor._

/** Tag trait to allow type matching on empty tiles of tile */
trait EmptyTile 

object EmptyTile {
  def apply(cellType: CellType, cols: Int, rows: Int): ConstantTile with EmptyTile = cellType match {
    case TypeBit    => new BitConstantTile(false, cols, rows) with EmptyTile
    case TypeByte   => new ByteConstantTile(byteNODATA, cols, rows) with EmptyTile
    case TypeUByte  => new UByteConstantTile(0, cols, rows) with EmptyTile
    case TypeShort  => new ShortConstantTile(shortNODATA, cols, rows) with EmptyTile
    case TypeUShort => new UShortConstantTile(0, cols, rows) with EmptyTile
    case TypeInt    => new IntConstantTile(NODATA, cols, rows) with EmptyTile
    case TypeFloat  => new FloatConstantTile(Float.NaN, cols, rows) with EmptyTile
    case TypeDouble => new DoubleConstantTile(Double.NaN, cols, rows) with EmptyTile
  }
}
