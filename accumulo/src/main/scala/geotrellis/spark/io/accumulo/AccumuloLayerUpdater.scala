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
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.merge._
import geotrellis.util._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect._

import AccumuloLayerWriter.Options

class AccumuloLayerUpdater(
  val instance: AccumuloInstance,
  val attributeStore: AttributeStore,
  layerReader: AccumuloLayerReader,
  options: Options
) extends LayerUpdater[LayerId] with LazyLogging {

  protected def _update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyBounds: KeyBounds[K], mergeFunc: (V, V) => V): Unit = {
    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerWriteError(id).initCause(e)
    }
    val layerWriter = new AccumuloLayerWriter(attributeStore, instance, header.tileTable, options)
    layerWriter.update(id, rdd, mergeFunc)
  }

  def overwrite[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M]): Unit = {
    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerWriteError(id).initCause(e)
    }
    val layerWriter = new AccumuloLayerWriter(attributeStore, instance, header.tileTable, options)
    layerWriter.overwrite(id, rdd)
  }
}

object AccumuloLayerUpdater {
  def apply(instance: AccumuloInstance, options: Options = Options.DEFAULT)(implicit sc: SparkContext): AccumuloLayerUpdater =
    new AccumuloLayerUpdater(
      instance = instance,
      attributeStore = AccumuloAttributeStore(instance.connector),
      layerReader = AccumuloLayerReader(instance),
      options = options
    )
}
