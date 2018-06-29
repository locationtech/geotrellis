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

package geotrellis.spark.io.s3.geotiff

import geotrellis.spark.io.s3.cog.byteReader
import geotrellis.spark.io.s3.conf.S3Config
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.raster.io.geotiff.{AutoHigherResolution, OverviewStrategy}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.io.hadoop.geotiff.{AttributeStore, GeoTiffLayerReader, GeoTiffMetadata}
import geotrellis.util.ByteReader
import geotrellis.util.annotations.experimental
import geotrellis.spark.io.s3.S3Client

import java.net.URI

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental case class S3GeoTiffLayerReader[M[T] <: Traversable[T]](
  attributeStore: AttributeStore[M, GeoTiffMetadata],
  layoutScheme: ZoomedLayoutScheme,
  resampleMethod: ResampleMethod = NearestNeighbor,
  strategy: OverviewStrategy = AutoHigherResolution,
  getS3Client: () => S3Client = () => S3Client.DEFAULT,
  defaultThreads: Int = S3GeoTiffLayerReader.defaultThreadCount
) extends GeoTiffLayerReader[M] {
  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri, getS3Client())
}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object S3GeoTiffLayerReader {
  val defaultThreadCount: Int = S3Config.threads.collection.readThreads
}
