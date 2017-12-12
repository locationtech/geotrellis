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

package geotrellis.spark.io.cog

import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.resample._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import spray.json._

import scala.reflect._

trait COGValueReader[ID] {
  val attributeStore: AttributeStore

  /** Produce a key value reader for a specific layer, prefetching layer metadata once at construction time */
  def reader[K: JsonFormat: SpatialComponent: ClassTag, V <: CellGrid: TiffMethods: ? => TileMergeMethods[V]](layerId: ID): Reader[K, V]

  def overzoomingReader[
    K: JsonFormat: SpatialComponent: ClassTag,
    V <: CellGrid: ? => TileResampleMethods[V]: TiffMethods: ? => TileMergeMethods[V]
  ](layerId: ID, resampleMethod: ResampleMethod = ResampleMethod.DEFAULT): Reader[K, V]
}


