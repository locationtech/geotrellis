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

package geotrellis.spark.streaming.io

import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.{Boundable, Bounds, LayerId, Metadata}
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.{AttributeStore, LayerUpdater, LayerWriter}
import geotrellis.spark.merge.Mergable
import geotrellis.util.GetComponent

import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

class GenericStreamingLayerWriter(val attributeStore: AttributeStore, val layerWriter: LayerWriter[LayerId], val layerUpdater: LayerUpdater[LayerId]) extends StreamingLayerWriter[LayerId] {
  protected def _write[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit = {
    if (!attributeStore.layerExists(id)) layerWriter.write[K, V, M](id, rdd, keyIndex)
    else layerUpdater.update[K, V, M](id, rdd)
  }
}

object GenericStreamingLayerWriter {
  def apply(attributeStore: AttributeStore, layerWriter: LayerWriter[LayerId], layerUpdater: LayerUpdater[LayerId]): StreamingLayerWriter[LayerId] =
    new GenericStreamingLayerWriter(attributeStore, layerWriter, layerUpdater)
}