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

package geotrellis.spark.io.hadoop

import geotrellis.tiling._
import geotrellis.layers.{LayerId, Metadata}
import geotrellis.layers._
import geotrellis.layers.hadoop.{HadoopAttributeStore, HadoopLayerHeader}
import geotrellis.layers.avro._
import geotrellis.layers.avro.codecs._
import geotrellis.layers.index.KeyIndex
import geotrellis.layers.merge.Mergable
import geotrellis.spark.io._
import geotrellis.util._

import com.typesafe.scalalogging.LazyLogging

import org.apache.hadoop.fs.Path
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import spray.json._

import scala.reflect._


class HadoopLayerWriter(
  rootPath: Path,
  val attributeStore: AttributeStore,
  indexInterval: Int = 4
) extends LayerWriter[LayerId] with LazyLogging {

  // Layer Updating
  def overwrite[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M]): Unit = {
    update(id, rdd, None)
  }

  def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], mergeFunc: (V, V) => V): Unit = {
    update(id, rdd, Some(mergeFunc))
  }

  private def update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]: Mergable
  ](
    id: LayerId,
    rdd: RDD[(K, V)] with Metadata[M],
    mergeFunc: Option[(V, V) => V]
  ): Unit = {
    val layerPath =
      try {
        new Path(rootPath,  s"${id.name}/${id.zoom}")
      } catch {
        case e: Exception =>
          throw new InvalidLayerIdError(id).initCause(e)
      }

    validateUpdate[HadoopLayerHeader, K, V, M](id, rdd.metadata) match {
      case Some(LayerAttributes(header, metadata, keyIndex, writerSchema)) =>
        val fn = mergeFunc match {
          case Some(fn) => fn
          case None => { (v1: V, v2: V) => v2 }
        }

        logger.info(s"Writing update for layer ${id} to ${header.path}")

        attributeStore.writeLayerAttributes(id, header, metadata, keyIndex, writerSchema)
        HadoopRDDWriter.update(rdd, layerPath, id, attributeStore, mergeFunc)

      case None =>
        logger.warn(s"Skipping update with empty bounds for $id.")
    }
  }

  // Layer Writing
  protected def _write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit = {
    val layerPath =
      try {
        new Path(rootPath,  s"${id.name}/${id.zoom}")
      } catch {
        case e: Exception =>
          throw new InvalidLayerIdError(id).initCause(e)
      }

    val header =
      HadoopLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        path = layerPath.toUri
      )
    val metadata = rdd.metadata

    try {
      attributeStore.writeLayerAttributes(id, header, metadata, keyIndex, KeyValueRecordCodec[K, V].schema)
      HadoopRDDWriter.write[K, V](rdd, layerPath, keyIndex, indexInterval)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object HadoopLayerWriter {
  def apply(rootPath: Path, attributeStore: AttributeStore): HadoopLayerWriter =
    new HadoopLayerWriter(
      rootPath = rootPath,
      attributeStore = attributeStore
    )

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopLayerWriter =
    apply(
      rootPath = rootPath,
      attributeStore = HadoopAttributeStore(rootPath)
    )
}
