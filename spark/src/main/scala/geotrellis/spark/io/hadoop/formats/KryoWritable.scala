/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.spark.io.hadoop.formats

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.spark.utils.KryoSerializer

import org.apache.hadoop.io.Writable
import java.io.{ DataInput, DataOutput }
import scala.reflect.ClassTag

class KryoWritable[T: ClassTag] extends Writable with Serializable {
  private var _bytes: Array[Byte] = Array()

  def set(thing: T): Unit = 
    _bytes = KryoSerializer.serialize[T](thing)

  def write(out: DataOutput): Unit = {
    val size = _bytes.size
    out.writeInt(size)
    out.write(_bytes, 0, size)
  }

  def readFields(in: DataInput): Unit = {
    val size = in.readInt
    _bytes = Array.ofDim[Byte](size)
    in.readFully(_bytes, 0, size)
  }

  def get(): T = 
    KryoSerializer.deserialize[T](_bytes)
}

object KryoWritable {
  def apply[T: ClassTag](thing: T) = {
    val tw = new KryoWritable[T]
    tw.set(thing)
    tw
  }
}
