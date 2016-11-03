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

package geotrellis.spark.io.accumulo

import geotrellis.spark._

import org.apache.accumulo.core.data.Key
import org.apache.hadoop.io.Text

object AccumuloKeyEncoder {
  final def long2Bytes(x: Long): Array[Byte] =
    Array[Byte](x>>56 toByte, x>>48 toByte, x>>40 toByte, x>>32 toByte, x>>24 toByte, x>>16 toByte, x>>8 toByte, x toByte)

  final def index2RowId(index: Long): Text = new Text(long2Bytes(index))

  def encode[K](id: LayerId, key: K, index: Long): Key =
    new Key(index2RowId(index), columnFamily(id))

  def getLocalityGroups(id: LayerId): Seq[String] = Seq(columnFamily(id))
}
