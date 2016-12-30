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

package geotrellis.pointcloud.spark.io.file

import io.pdal._

import geotrellis.pointcloud.spark.io._
import geotrellis.pointcloud.spark.io.{FilteringLayerReader => PCFilteringLayerReader}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.file._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.util._

import org.apache.spark.SparkContext
import spray.json.JsonFormat
import com.typesafe.scalalogging.LazyLogging

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from a filesystem.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 */
class FileLayerReader(
  val attributeStore: AttributeStore,
  catalogPath: String
)(implicit sc: SparkContext) extends PCFilteringLayerReader[LayerId] with LazyLogging {

  val defaultNumPartitions = sc.defaultParallelism

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, tileQuery: LayerQuery[K, M], numPartitions: Int, filterIndexOnly: Boolean) = {
    if(!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[FileLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val layerPath = header.path

    val queryKeyBounds = tileQuery(metadata)
    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = KeyPathGenerator(catalogPath, layerPath, maxWidth)
    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
    val rdd = FileRDDReader.read[K](keyPath, queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema), Some(numPartitions))

    new ContextRDD(rdd, metadata)
  }
}

object FileLayerReader {
  def apply(attributeStore: AttributeStore, catalogPath: String)(implicit sc: SparkContext): FileLayerReader =
    new FileLayerReader(attributeStore, catalogPath)

  def apply(catalogPath: String)(implicit sc: SparkContext): FileLayerReader =
    apply(new FileAttributeStore(catalogPath), catalogPath)

  def apply(attributeStore: FileAttributeStore)(implicit sc: SparkContext): FileLayerReader =
    apply(attributeStore, attributeStore.catalogPath)
}
