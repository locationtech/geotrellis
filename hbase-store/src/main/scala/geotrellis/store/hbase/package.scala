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

package geotrellis.store

import geotrellis.layers.LayerId

import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.util.Bytes

package object hbase {
  val hbaseTileColumnFamily = "tiles"
  val hbaseSEP = "__.__"
  def hbaseLayerIdString(layerId: LayerId): String = s"${layerId.name}${hbaseSEP}${layerId.zoom}|"

  implicit def stringToTableName(str: String): TableName = TableName.valueOf(str)
  implicit def stringToBytes(str: String): Array[Byte] = Bytes.toBytes(str)
  implicit def intToBytes(i: Int): Array[Byte] = Bytes.toBytes(i)
  implicit def longToBytes(l: Long): Array[Byte] = Bytes.toBytes(l)
}
