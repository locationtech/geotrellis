/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.store.file.geotiff

import geotrellis.layer.ZoomedLayoutScheme
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.raster.io.geotiff.OverviewStrategy
import geotrellis.spark.store.hadoop.geotiff._
import geotrellis.store.util.IORuntimeTransient
import geotrellis.util.annotations.experimental

import cats.effect._

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental class FileGeoTiffLayerReader[M[T] <: Traversable[T]](
  val attributeStore: AttributeStore[M, GeoTiffMetadata],
  val layoutScheme: ZoomedLayoutScheme,
  val resampleMethod: ResampleMethod = NearestNeighbor,
  val strategy: OverviewStrategy = OverviewStrategy.DEFAULT,
  runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
) extends GeoTiffLayerReader[M] {
  implicit lazy val ioRuntime: unsafe.IORuntime = runtime
}

@experimental object FileGeoTiffLayerReader {
  def apply[M[T] <: Traversable[T]](
    attributeStore: AttributeStore[M, GeoTiffMetadata],
    layoutScheme: ZoomedLayoutScheme,
    resampleMethod: ResampleMethod = NearestNeighbor,
    strategy: OverviewStrategy = OverviewStrategy.DEFAULT,
    runtime: => unsafe.IORuntime = IORuntimeTransient.IORuntime
  ): FileGeoTiffLayerReader[M] =
    new FileGeoTiffLayerReader(attributeStore, layoutScheme, resampleMethod, strategy, runtime)
}
