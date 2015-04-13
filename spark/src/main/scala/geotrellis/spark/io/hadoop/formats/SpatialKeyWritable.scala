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

import java.io._

class SpatialKeyWritable() extends Writable
    with IndexedKeyWritable[SpatialKey]
    with WritableComparable[SpatialKeyWritable]
    with Serializable {
  private var _index: Long = 0L
  private var _value: SpatialKey = null

  def index = _index
  def key = _value

  def set(i: Long, k: SpatialKey): Unit = {
    _index = i
    _value = k
  }

  def get(): (Long, SpatialKey) = (_index, _value)

  def write(out: DataOutput): Unit = {
    out.writeLong(_index)
    out.writeInt(_value.col)
    out.writeInt(_value.row)
  }

  def readFields(in: DataInput): Unit = {
    _index = in.readLong
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
    if(this._index < other._index) -1
    else if (this._index > other._index) 1
    else {
      if(SpatialKey.ordering.lt(this._value, other._value)) -1
      else if(this._value == other._value) 0
      else 1
    }
}

object SpatialKeyWritable {
  implicit def ordering[A <: SpatialKeyWritable]: Ordering[A] =
    Ordering.by(w => w.get)

  def apply(index: Long, spatialKey: SpatialKey): SpatialKeyWritable = {
    val w = new SpatialKeyWritable
    w.set(index, spatialKey)
    w
  }
}
