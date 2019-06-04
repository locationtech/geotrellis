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

package geotrellis.spark.store.file

import geotrellis.layers.LayerId
import geotrellis.layers.avro._
import geotrellis.layers.LayerReindexer
import geotrellis.layers.file._
import geotrellis.layers.index._
import geotrellis.spark.store.GenericLayerReindexer

import org.apache.spark.SparkContext

import spray.json.JsonFormat

import scala.reflect.ClassTag

object FileLayerReindexer {
  def apply(attributeStore: FileAttributeStore)(implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val layerReader  = FileLayerReader(attributeStore)
    val layerWriter  = FileLayerWriter(attributeStore)
    val layerDeleter = FileLayerDeleter(attributeStore)
    val layerCopier  = FileLayerCopier(attributeStore)

    GenericLayerReindexer[FileLayerHeader](attributeStore, layerReader, layerWriter, layerDeleter, layerCopier)
  }

  def apply(catalogPath: String)(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply(FileAttributeStore(catalogPath))

}
