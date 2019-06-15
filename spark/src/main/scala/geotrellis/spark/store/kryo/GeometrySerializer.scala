/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.store.kryo

import geotrellis.vector.Geometry
import geotrellis.vector.io.wkb.WKB

import com.esotericsoftware.kryo.{Kryo, Serializer}
import com.esotericsoftware.kryo.io.{Input, Output}

class GeometrySerializer[G <: Geometry] extends Serializer[G] {
  override def write(kryo: Kryo, output: Output, geom: G): Unit = {
    val wkb = WKB.write(geom)

    output.writeInt(wkb.length)
    output.writeBytes(wkb)
  }

  override def read(kryo: Kryo, input: Input, t: Class[G]): G = {
    val bufLength = input.readInt
    val wkb = input.readBytes(bufLength)

    WKB.read(wkb).asInstanceOf[G]
  }
}
