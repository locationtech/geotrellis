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

package geotrellis.layers.accumulo

import geotrellis.layers.LayerId

import org.apache.accumulo.core.data.Key
import org.apache.hadoop.io.Text


object AccumuloKeyEncoder {
  final def long2Bytes(x: BigInt): Array[Byte] = {
    val bytes1: Array[Byte] = x.toByteArray
    val bytes2: Array[Byte] = Stream.continually(0.toByte).take(8 - bytes1.length).toArray
    (bytes2 ++ bytes1) // XXX
  }

  final def index2RowId(index: BigInt): Text = {
    new Text(long2Bytes(index))
  }

  def encode[K](id: LayerId, key: K, index: BigInt): Key =
    new Key(index2RowId(index), columnFamily(id))

  def getLocalityGroups(id: LayerId): Seq[String] = Seq(columnFamily(id))
}
