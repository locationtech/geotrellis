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

package geotrellis.spark.store.geowave

import geotrellis.util.annotations.experimental

import mil.nga.giat.geowave.core.index.Persistable
import mil.nga.giat.geowave.core.index.PersistenceUtils
import org.apache.hadoop.io.ObjectWritable
import org.apache.spark.util.Utils

import java.io.{ObjectInputStream, ObjectOutputStream}


/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental class SerializablePersistable[T <: Persistable](@transient var t: T)
    extends Serializable {

  /** $experimental */
  @experimental def value: T = t

  /** $experimental */
  @experimental override def toString: String = t.toString

  @experimental private def writeObject(out: ObjectOutputStream): Unit = {
    val bytes = PersistenceUtils.toBinary(t)
    out.writeInt(bytes.length)
    out.write(bytes)
  }

  @experimental private def readObject(in: ObjectInputStream): Unit = {
    val length = in.readInt()
    val bytes = new Array[Byte](length)
    in.readFully(bytes)
    t=PersistenceUtils.fromBinary(bytes, classOf[Persistable]).asInstanceOf[T]
  }
}
