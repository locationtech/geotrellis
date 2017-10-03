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

package geotrellis.spark.io.hbase

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

class HBaseLayerUpdater(
  val instance: HBaseInstance,
  val attributeStore: AttributeStore,
  layerReader: HBaseLayerReader
) extends LayerUpdater[LayerId] with LazyLogging {

  def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], mergeFunc: (V, V) => V): Unit = {
    val table = attributeStore.readHeader[HBaseLayerHeader](id).tileTable
    val layerWriter = new HBaseLayerWriter(attributeStore, instance, table)
    implicit val sc = rdd.sparkContext
    layerWriter.update(id, rdd, mergeFunc)
  }

  def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M]): Unit = {
    val table = attributeStore.readHeader[HBaseLayerHeader](id).tileTable
    val layerWriter = new HBaseLayerWriter(attributeStore, instance, table)
    implicit val sc = rdd.sparkContext
    layerWriter.update(id, rdd)
  }

  def overwrite[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M]): Unit = {
    val table = attributeStore.readHeader[HBaseLayerHeader](id).tileTable
    val layerWriter = new HBaseLayerWriter(attributeStore, instance, table)
    implicit val sc = rdd.sparkContext
    layerWriter.overwrite(id, rdd)
  }
}

object HBaseLayerUpdater {
  def apply(instance: HBaseInstance)(implicit sc: SparkContext): HBaseLayerUpdater =
    new HBaseLayerUpdater(
      instance = instance,
      attributeStore = HBaseAttributeStore(instance),
      layerReader = HBaseLayerReader(instance)
    )

  def apply(attributeStore: HBaseAttributeStore)(implicit sc: SparkContext): HBaseLayerUpdater =
    new HBaseLayerUpdater(
      instance = attributeStore.instance,
      attributeStore = attributeStore,
      layerReader = HBaseLayerReader(attributeStore)
    )
}
