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

package geotrellis.store.hbase

import geotrellis.layers.LayerId
import geotrellis.spark._

object HBaseKeyEncoder {
  def encode(id: LayerId, index: BigInt, trailingByte: Boolean = false): Array[Byte] = {
    val bytes3 = (index.toByteArray: Array[Byte])
    val bytes2: Array[Byte] = Stream.continually(0.toByte).take(8 - bytes3.length).toArray
    val bytes1 = (s"${hbaseLayerIdString(id)}": Array[Byte])
    val result: Array[Byte] = bytes1 ++ bytes2 ++ bytes3
    if(trailingByte) result :+ 0.toByte else result
  }
}
