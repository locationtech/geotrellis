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

package geotrellis.spark.store.hadoop.cog

import geotrellis.layers.{COGLayerType, LayerHeader}
import geotrellis.layers.hadoop.{HadoopAttributeStore, HadoopLayerHeader}
import geotrellis.layers.hadoop._
import geotrellis.spark.store.cog._
import geotrellis.spark.store.hadoop._

import org.apache.spark.SparkContext

import java.net.URI


class COGHadoopAttributeStoreSpec extends COGAttributeStoreSpec {
  lazy val attributeStore = HadoopAttributeStore.apply(outputLocalPath)
  lazy val header = HadoopLayerHeader("geotrellis.tiling.SpatialKey", "geotrellis.raster.Tile", new URI(outputLocalPath), COGLayerType)
}
