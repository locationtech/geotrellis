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

package geotrellis.spark.store.hbase

import geotrellis.layers._
import geotrellis.store.hbase._
import geotrellis.spark._
import geotrellis.spark.io._
import org.apache.spark.SparkContext

object HBaseLayerMover {
  def apply(
    layerCopier: HBaseLayerCopier,
    layerDeleter: HBaseLayerDeleter
  ): LayerMover[LayerId] =
    new GenericLayerMover(layerCopier, layerDeleter)

  def apply(
   instance: HBaseInstance
  )(implicit sc: SparkContext): LayerMover[LayerId] = {
    val attributeStore = HBaseAttributeStore(instance)
    apply(
      layerCopier = HBaseLayerCopier(instance),
      layerDeleter = HBaseLayerDeleter(attributeStore)
    )
  }

  def apply(
    instance: HBaseInstance,
    table: String
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = HBaseLayerCopier(instance, table),
      layerDeleter = HBaseLayerDeleter(instance)
    )

  def apply(
    attributeStore: HBaseAttributeStore,
    table: String
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = HBaseLayerCopier(attributeStore.instance, table),
      layerDeleter = HBaseLayerDeleter(attributeStore.instance)
    )
}
