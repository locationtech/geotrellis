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

import geotrellis.tiling.{Boundable, Bounds, EmptyBounds, KeyBounds}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.layers.{LayerId, Metadata}
import geotrellis.layers._
import geotrellis.layers.avro._
import geotrellis.layers.hadoop.{HadoopAttributeStore, HadoopLayerHeader}
import geotrellis.util._

import com.typesafe.scalalogging.LazyLogging

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V       Type of RDD Value (ex: Tile or MultibandTile )
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 */
class HadoopLayerReader(
  val attributeStore: AttributeStore
)(implicit sc: SparkContext)
  extends FilteringLayerReader[LayerId] with LazyLogging {

  val defaultNumPartitions = sc.defaultParallelism

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, tileQuery: LayerQuery[K, M], numPartitions: Int, indexFilterOnly: Boolean): RDD[(K, V)] with Metadata[M] = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val layerPath = new Path(header.path)
    val keyBounds = metadata.getComponent[Bounds[K]].getOrElse(throw new LayerEmptyBoundsError(id))
    val queryKeyBounds = tileQuery(metadata)
    val layerMetadata = metadata.setComponent[Bounds[K]](queryKeyBounds.foldLeft(EmptyBounds: Bounds[K])(_ combine _))

    val rdd: RDD[(K, V)] =
      if (queryKeyBounds == Seq(keyBounds)) {
        HadoopRDDReader.readFully(layerPath, Some(writerSchema))
      } else {
        val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
        HadoopRDDReader.readFiltered(layerPath, queryKeyBounds, decompose, indexFilterOnly, Some(writerSchema))
      }

    new ContextRDD[K, V, M](rdd, layerMetadata)
  }
}

object HadoopLayerReader {
  def apply(attributeStore: HadoopAttributeStore)(implicit sc: SparkContext) =
    new HadoopLayerReader(attributeStore)

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopLayerReader =
    apply(HadoopAttributeStore(rootPath))
}
