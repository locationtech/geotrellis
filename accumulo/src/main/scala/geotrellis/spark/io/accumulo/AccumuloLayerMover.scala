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

package geotrellis.spark.io.accumulo

import geotrellis.layers._
import geotrellis.layers.accumulo._
import geotrellis.layers.avro._
import geotrellis.layers.index._
import geotrellis.layers.json._
import geotrellis.spark.io._
import geotrellis.util._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import spray.json.JsonFormat

import scala.reflect.ClassTag

object AccumuloLayerMover {
  def apply(
    layerCopier: AccumuloLayerCopier,
    layerDeleter: AccumuloLayerDeleter
  ): LayerMover[LayerId] =
    new GenericLayerMover(layerCopier, layerDeleter)

  def apply(
   instance: AccumuloInstance
  )(implicit sc: SparkContext): LayerMover[LayerId] = {
    val attributeStore = AccumuloAttributeStore(instance.connector)
    apply(
      layerCopier = AccumuloLayerCopier(instance),
      layerDeleter = AccumuloLayerDeleter(attributeStore)
    )
  }

  def apply(
    instance: AccumuloInstance,
    table: String,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = AccumuloLayerCopier(instance, table, options),
      layerDeleter = AccumuloLayerDeleter(instance)
    )

  def apply(
    instance: AccumuloInstance,
    table: String
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = AccumuloLayerCopier(instance, table),
      layerDeleter = AccumuloLayerDeleter(instance)
    )

  def apply(
    instance: AccumuloInstance,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = AccumuloLayerCopier(instance, options),
      layerDeleter = AccumuloLayerDeleter(instance)
    )
}
