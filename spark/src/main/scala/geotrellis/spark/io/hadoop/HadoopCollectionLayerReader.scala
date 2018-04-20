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

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.util._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import spray.json._

import scala.reflect.ClassTag

/**
  * Handles reading raster RDDs and their metadata from S3.
  *
  * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
  */
class HadoopCollectionLayerReader(
  val attributeStore: AttributeStore,
  conf: Configuration,
  maxOpenFiles: Int = 16
)
  extends CollectionLayerReader[LayerId] with LazyLogging {

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rasterQuery: LayerQuery[K, M], indexFilterOnly: Boolean): Seq[(K, V)] with Metadata[M] = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val layerPath = new Path(header.path)
    val keyBounds = metadata.getComponent[Bounds[K]].getOrElse(throw new LayerEmptyBoundsError(id))
    val queryKeyBounds = rasterQuery(metadata)
    val layerMetadata = metadata.setComponent[Bounds[K]](queryKeyBounds.foldLeft(EmptyBounds: Bounds[K])(_ combine _))
    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)

    val seq = HadoopCollectionReader(maxOpenFiles).read[K, V](layerPath, conf, queryKeyBounds, decompose, indexFilterOnly, Some(writerSchema))

    new ContextCollection[K, V, M](seq, layerMetadata)
  }
}

object HadoopCollectionLayerReader {
  def apply(attributeStore: HadoopAttributeStore): HadoopCollectionLayerReader =
    new HadoopCollectionLayerReader(attributeStore, attributeStore.hadoopConfiguration)

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopCollectionLayerReader =
    apply(HadoopAttributeStore(rootPath))

  def apply(rootPath: Path, conf: Configuration): HadoopCollectionLayerReader =
    apply(HadoopAttributeStore(rootPath, conf))
}
