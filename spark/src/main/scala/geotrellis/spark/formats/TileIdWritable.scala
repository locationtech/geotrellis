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

package geotrellis.spark.formats

import org.apache.hadoop.io.LongWritable

import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class TileIdWritable extends LongWritable with Serializable {
  override def equals(that: Any): Boolean =
    that match {
      case other: TileIdWritable => other.get == this.get
      case _ => false
    }

  override def hashCode = get.hashCode
  
  private def writeObject(out: ObjectOutputStream) {
    out.defaultWriteObject()
    out.writeLong(get)
  }

  private def readObject(in: ObjectInputStream) {
    in.defaultReadObject()
    set(in.readLong)
  }
}

object TileIdWritable {
  def apply(value: Long): TileIdWritable = {
    val tw = new TileIdWritable
    tw.set(value)
    tw
  }
  def apply(tw: TileIdWritable): TileIdWritable = {
    TileIdWritable(tw.get)
  }
}
