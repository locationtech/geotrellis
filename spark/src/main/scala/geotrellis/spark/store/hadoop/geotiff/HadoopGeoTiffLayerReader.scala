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

package geotrellis.spark.store.hadoop.geotiff

import geotrellis.tiling.ZoomedLayoutScheme
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.raster.io.geotiff.{AutoHigherResolution, OverviewStrategy}
import geotrellis.layers.hadoop.cog.byteReader
import geotrellis.layers.hadoop.conf.HadoopConfig
import geotrellis.util.ByteReader
import geotrellis.util.annotations.experimental

import org.apache.hadoop.conf.Configuration

import java.net.URI


/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental case class HadoopGeoTiffLayerReader[M[T] <: Traversable[T]](
  attributeStore: AttributeStore[M, GeoTiffMetadata],
  layoutScheme: ZoomedLayoutScheme,
  resampleMethod: ResampleMethod = NearestNeighbor,
  strategy: OverviewStrategy = AutoHigherResolution,
  conf: Configuration = new Configuration,
  defaultThreads: Int = HadoopGeoTiffLayerReader.defaultThreadCount
) extends GeoTiffLayerReader[M] {
  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri, conf)
}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object HadoopGeoTiffLayerReader {
  val defaultThreadCount: Int = HadoopConfig.threads.collection.readThreads
}
