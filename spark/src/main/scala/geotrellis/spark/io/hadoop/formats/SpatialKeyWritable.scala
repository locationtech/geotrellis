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

import org.apache.hadoop.io._

import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class SpatialKeyWritable() extends Writable
                              with WritableComparable[LongWritable] 
                              with Serializable {
  private var _value: SpatialKey = null

  def set(spatialKey: SpatialKey): Unit = _value = spatialKey
  def get(): SpatialKey = _value

  private def write(out: ObjectOutputStream) {
    out.writeInt(_value.col)
    out.writeInt(_value.row)
  }

  private def readFields(in: ObjectInputStream) {
    val col = in.readInt
    val row = in.readInt
    _value = SpatialKey(col, row)
  }

  override def equals(that: Any): Boolean =
    that match {
      case other: SpatialKeyWritable => other.get == this.get
      case _ => false
    }

  override def hashCode = get.hashCode

  def compareTo(other: SpatialKeyWritable): Int =
    if(SpatialKey.ordering.lt(this._value, other._value)) -1
    else if(this._value == other._value) 0 
    else 1
}

object SpatialKeyWritable {
  implicit def ordering[A <: SpatialKeyWritable]: Ordering[A] =
    Ordering.by(w => w.get)

  implicit def spatialKeytoWritable(spatialKey: SpatialKey): SpatialKeyWritable = 
    apply(spatialKey)

  def apply(spatialKey: SpatialKey): SpatialKeyWritable = {
    val w = new SpatialKeyWritable
    w.set(spatialKey)
    w
  }
}
