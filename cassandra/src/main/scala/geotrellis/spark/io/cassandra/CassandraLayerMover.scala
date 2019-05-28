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

import geotrellis.layers.LayerId
import geotrellis.spark._
import geotrellis.spark.io._
import org.apache.spark.SparkContext

object CassandraLayerMover {
  def apply(
    layerCopier: CassandraLayerCopier,
    layerDeleter: CassandraLayerDeleter
  ): LayerMover[LayerId] =
    new GenericLayerMover(layerCopier, layerDeleter)

  def apply(
   instance: CassandraInstance
  )(implicit sc: SparkContext): LayerMover[LayerId] = {
    val attributeStore = CassandraAttributeStore(instance)
    apply(
      layerCopier = CassandraLayerCopier(instance),
      layerDeleter = CassandraLayerDeleter(attributeStore)
    )
  }

  def apply(
    instance: CassandraInstance,
    keyspace: String,
    table: String
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = CassandraLayerCopier(instance, keyspace, table),
      layerDeleter = CassandraLayerDeleter(instance)
    )

  def apply(
    attributeStore: CassandraAttributeStore,
    keyspace: String,
    table: String
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = CassandraLayerCopier(attributeStore.instance, keyspace, table),
      layerDeleter = CassandraLayerDeleter(attributeStore.instance)
    )
}
