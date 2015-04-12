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
import org.joda.time.{DateTime, DateTimeZone}

import java.io._

class SpaceTimeKeyWritable() extends Writable
                              with WritableComparable[SpaceTimeKeyWritable] 
                              with Serializable {
  private var _index: Long = 0L
  private var _value: SpaceTimeKey = null

  def set(index: Long, key: SpaceTimeKey): Unit = {
    _index = index
    _value = key
  }

  def get(): (Long, SpaceTimeKey) = (_index, _value)

  def write(out: DataOutput): Unit = {
    out.writeLong(_index)
    out.writeLong(_value.temporalKey.time.toInstant.getMillis)
    out.writeInt(_value.spatialKey.col)
    out.writeInt(_value.spatialKey.row)
  }

  def readFields(in: DataInput): Unit = {
    _index = in.readLong
    val millis = in.readLong
    val col = in.readInt
    val row = in.readInt
    _value = SpaceTimeKey(SpatialKey(col, row), new DateTime(millis, DateTimeZone.UTC))
  }

  override def equals(that: Any): Boolean =
    that match {
      case other: SpaceTimeKeyWritable => other.get == this.get
      case _ => false
    }

  override def hashCode = get.hashCode

  def compareTo(other: SpaceTimeKeyWritable): Int =
    if(this._index < other._index) -1
    else if (this._index > other._index) 1
    else {
      if(SpaceTimeKey.ordering.lt(this._value, other._value)) -1
      else if(this._value == other._value) 0
      else 1
    }
}

object SpaceTimeKeyWritable {
  implicit def ordering[A <: SpaceTimeKeyWritable]: Ordering[A] =
    Ordering.by(w => w.get)

  def apply(index: Long, spaceTimeKey: SpaceTimeKey): SpaceTimeKeyWritable = {
    val w = new SpaceTimeKeyWritable
    w.set(index, spaceTimeKey)
    w
  }
}
