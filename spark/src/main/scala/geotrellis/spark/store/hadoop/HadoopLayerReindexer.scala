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

package geotrellis.spark.store.hadoop

import geotrellis.store._
import geotrellis.store.hadoop._
import geotrellis.spark.store._

import org.apache.spark.SparkContext
import org.apache.hadoop.fs.Path

object HadoopLayerReindexer {
  def apply(rootPath: Path, attributeStore: HadoopAttributeStore)(implicit sc: SparkContext): LayerReindexer[LayerId] =
    GenericLayerReindexer(
      attributeStore = attributeStore,
      layerReader    = HadoopLayerReader(rootPath),
      layerWriter    = HadoopLayerWriter(rootPath),
      layerDeleter   = HadoopLayerDeleter(rootPath, attributeStore.conf),
      layerCopier    = HadoopLayerCopier(rootPath, attributeStore)
    )

  def apply(attributeStore: HadoopAttributeStore)(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply(attributeStore.rootPath, attributeStore)

  def apply(rootPath: Path)(implicit sc: SparkContext): LayerReindexer[LayerId] =
    apply(rootPath, HadoopAttributeStore(rootPath))
}
