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

import geotrellis.tiling.ZoomedLayoutScheme
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.raster.io.geotiff.{AutoHigherResolution, OverviewStrategy}
import geotrellis.layers.file._
import geotrellis.layers.file.cog.byteReader
import geotrellis.layers.file.conf.FileConfig
import geotrellis.spark.store.hadoop.geotiff._
import geotrellis.util.ByteReader
import geotrellis.util.annotations.experimental

import java.net.URI


/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental case class FileGeoTiffLayerReader[M[T] <: Traversable[T]](
  attributeStore: AttributeStore[M, GeoTiffMetadata],
  layoutScheme: ZoomedLayoutScheme,
  resampleMethod: ResampleMethod = NearestNeighbor,
  strategy: OverviewStrategy = AutoHigherResolution,
  defaultThreads: Int = FileGeoTiffLayerReader.defaultThreadCount
) extends GeoTiffLayerReader[M] {
  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri)
}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object FileGeoTiffLayerReader {
  val defaultThreadCount: Int = FileConfig.threads.collection.readThreads
}
