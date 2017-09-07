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

package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.merge._
import geotrellis.util._

import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext

import spray.json._

import scala.reflect._

class S3LayerUpdater(
  val attributeStore: AttributeStore,
  layerReader: S3LayerReader
) extends LayerUpdater[LayerId] with LazyLogging {

  def rddWriter: S3RDDWriter = S3RDDWriter
  def _rddWriter(): S3RDDWriter = rddWriter

  class InnerS3LayerWriter(
    attributeStore: AttributeStore,
    bucket: String,
    keyPrefix: String
  ) extends S3LayerWriter(attributeStore, bucket, keyPrefix) {
    override def rddWriter() = _rddWriter
  }

  val as = attributeStore.asInstanceOf[S3AttributeStore]
  val layerWriter = new InnerS3LayerWriter(as, as.bucket, as.prefix)
  val sc: SparkContext = layerReader.sparkContext

  def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], mergeFunc: (V, V) => V): Unit = {
    rdd.metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        layerWriter._update(sc, id, rdd, keyBounds, Some(mergeFunc), layerReader)
      case EmptyBounds =>
        throw new EmptyBoundsError(s"Cannot update layer $id with a layer with empty bounds.")
    }
  }

  def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M]): Unit = {
    rdd.metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        layerWriter._update(sc, id, rdd, keyBounds, Some({(_: V, v: V) => v}), layerReader)
      case EmptyBounds =>
        throw new EmptyBoundsError(s"Cannot update layer $id with a layer with empty bounds.")
    }
  }

  def overwrite[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M]): Unit = {
    rdd.metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        layerWriter._update(sc, id, rdd, keyBounds, None, layerReader)
      case EmptyBounds =>
        throw new EmptyBoundsError(s"Cannot overwrite layer $id with a layer with empty bounds.")
    }
  }
}

object S3LayerUpdater {
  def apply(
    bucket: String,
    prefix: String
  )(implicit sc: SparkContext): S3LayerUpdater =
    new S3LayerUpdater(
      S3AttributeStore(bucket, prefix),
      S3LayerReader(bucket, prefix)
    )
}
