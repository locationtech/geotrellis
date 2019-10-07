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

package geotrellis.spark.reproject

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.reproject._
import geotrellis.raster.resample._
import geotrellis.raster.stitch._
import geotrellis.layer._
import geotrellis.spark._
import geotrellis.spark.buffer._
import geotrellis.spark.ingest._
import geotrellis.proj4._
import geotrellis.util.MethodExtensions

import spire.math.Integral

import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect.ClassTag


class TileRDDReprojectMethods[
  K: SpatialComponent: Boundable: ClassTag,
  V <: CellGrid[Int]: ClassTag: RasterRegionReproject: Stitcher: (* => CropMethods[V]): (* => TileMergeMethods[V]): (* => TilePrototypeMethods[V])
](val self: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) extends MethodExtensions[RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] {

  def reproject(
    destCrs: CRS,
    layoutScheme: LayoutScheme,
    resampleMethod: ResampleMethod = NearestNeighbor,
    partitioner: Option[Partitioner] = None,
    bufferSize: Option[Int] = None,
    errorThreshold: Double = 0.125
  ): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) = {
    TileRDDReproject(self, destCrs, Left(layoutScheme), resampleMethod, partitioner, bufferSize, errorThreshold)
  }

  def reprojectToLayout(
    destCrs: CRS,
    layoutDefinition: LayoutDefinition,
    resampleMethod: ResampleMethod = NearestNeighbor,
    partitioner: Option[Partitioner] = None,
    bufferSize: Option[Int] = None,
    errorThreshold: Double = 0.125
  ): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] =  {
    TileRDDReproject(self, destCrs, Right(layoutDefinition), resampleMethod, partitioner, bufferSize, errorThreshold)._2
  }
}