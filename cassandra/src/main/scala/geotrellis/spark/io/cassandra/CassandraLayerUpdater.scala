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

package geotrellis.spark.io.cassandra

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

class CassandraLayerUpdater(
  val instance: CassandraInstance,
  val attributeStore: AttributeStore,
  layerReader: CassandraLayerReader
) extends LayerUpdater[LayerId] with LazyLogging {

  implicit private val sc = layerReader.sparkContext

  def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], mergeFunc: (V, V) => V): Unit = {
    val CassandraLayerHeader(_, _, keyspace, table) = attributeStore.readHeader[CassandraLayerHeader](id)
    val layerWriter = new CassandraLayerWriter(attributeStore, instance, keyspace, table)
    layerWriter.update(id, rdd, mergeFunc)
  }

  def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M]): Unit = {
    val CassandraLayerHeader(_, _, keyspace, table) = attributeStore.readHeader[CassandraLayerHeader](id)
    val layerWriter = new CassandraLayerWriter(attributeStore, instance, keyspace, table)
    layerWriter.update(id, rdd)
  }

  def overwrite[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M]): Unit = {
    val CassandraLayerHeader(_, _, keyspace, table) = attributeStore.readHeader[CassandraLayerHeader](id)
    val layerWriter = new CassandraLayerWriter(attributeStore, instance, keyspace, table)
    layerWriter.overwrite(id, rdd)
  }

}

object CassandraLayerUpdater {
  def apply(instance: CassandraInstance)(implicit sc: SparkContext): CassandraLayerUpdater =
    new CassandraLayerUpdater(
      instance = instance,
      attributeStore = CassandraAttributeStore(instance),
      layerReader = CassandraLayerReader(instance)
    )

  def apply(attributeStore: CassandraAttributeStore)(implicit sc: SparkContext): CassandraLayerUpdater =
    new CassandraLayerUpdater(
      instance = attributeStore.instance,
      attributeStore = attributeStore,
      layerReader = CassandraLayerReader(attributeStore)
    )
}
