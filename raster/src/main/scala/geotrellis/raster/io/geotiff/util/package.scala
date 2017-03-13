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

package geotrellis.raster.io.geotiff

import geotrellis.raster._
import java.nio.ByteBuffer

package object util extends ArrayExtensions
  with ByteReaderExtensions
  with ByteInverter
  with GDALNoDataParser {

  implicit class ShortArrayToByte(val arr: Array[Short]) extends AnyVal {
    def toArrayByte(): Array[Byte] = {
      val result = new Array[Byte](arr.size * ShortConstantNoDataCellType.bytes)
      val bytebuff = ByteBuffer.wrap(result)
      bytebuff.asShortBuffer.put(arr)
      result
    }
  }

  implicit class IntArrayToByte(val arr: Array[Int]) extends AnyVal {
    def toArrayByte(): Array[Byte] = {
      val result = new Array[Byte](arr.size * IntConstantNoDataCellType.bytes)
      val bytebuff = ByteBuffer.wrap(result)
      bytebuff.asIntBuffer.put(arr)
      result
    }
  }

  implicit class FloatArrayToByte(val arr: Array[Float]) extends AnyVal {
    def toArrayByte(): Array[Byte] = {
      val result = new Array[Byte](arr.size * FloatConstantNoDataCellType.bytes)
      val bytebuff = ByteBuffer.wrap(result)
      bytebuff.asFloatBuffer.put(arr)
      result
    }
  }

  implicit class DoubleArrayToByte(val arr: Array[Double]) extends AnyVal {
    def toArrayByte(): Array[Byte] = {
      val result = new Array[Byte](arr.size * DoubleConstantNoDataCellType.bytes)
      val bytebuff = ByteBuffer.wrap(result)
      bytebuff.asDoubleBuffer.put(arr)
      result
    }
  }

}
