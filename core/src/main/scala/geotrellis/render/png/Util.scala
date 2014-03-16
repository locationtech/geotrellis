/**************************************************************************
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
 **************************************************************************/

package geotrellis.render.png

import java.nio.ByteBuffer

import geotrellis._

object Util {
  @inline final def byte(i:Int):Byte = i.toByte
  @inline final def shift(n:Int, i:Int):Byte = byte(n >> i)

  /**
   * ByteBuffer boiler-plate stuff below.
   */
  def initByteBuffer32(bb:ByteBuffer, d:Array[Int], size:Int) {
    var j = 0
    while (j < size) {
      val z = d(j)
      bb.put(byte(z >> 24))
      bb.put(byte(z >> 16))
      bb.put(byte(z >> 8))
      bb.put(byte(z))
      j += 1
    }
  }

  def initByteBuffer24(bb:ByteBuffer, d:Array[Int], size:Int) {
    var j = 0
    while (j < size) {
      val z = d(j)
      bb.put(byte(z >> 16))
      bb.put(byte(z >> 8))
      bb.put(byte(z))
      j += 1
    }
  }

  def initByteBuffer16(bb:ByteBuffer, d:Array[Int], size:Int) {
    var j = 0
    while (j < size) {
      val z = d(j)
      bb.put(byte(z >> 8))
      bb.put(byte(z))
      j += 1
    }
  }

  def initByteBuffer8(bb:ByteBuffer, d:Array[Int], size:Int) {
    var j = 0
    while (j < size) {
      bb.put(byte(d(j)))
      j += 1
    }
  }
}
