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
import geotrellis.raster.stitch._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.proj4._
import geotrellis.util.MethodExtensions

import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect.ClassTag


class TileRDDReprojectMethods[
  K: SpatialComponent: Boundable: ClassTag,
  V <: CellGrid[Int]: ClassTag: RasterRegionReproject: Stitcher: (? => CropMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
](val self: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) extends MethodExtensions[RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] {
  import Reproject.Options

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme, options: Options, partitioner: Option[Partitioner]): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    TileRDDReproject(self, destCrs, Left(layoutScheme), options, partitioner)

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme, partitioner: Option[Partitioner]): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    TileRDDReproject(self, destCrs, Left(layoutScheme), Options.DEFAULT, partitioner)

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme, options: Options): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    TileRDDReproject(self, destCrs, Left(layoutScheme), options, None)

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(destCrs, layoutScheme, Options.DEFAULT, None)

  def reproject(zoomedLayoutScheme: ZoomedLayoutScheme, options: Options): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(zoomedLayoutScheme.crs, zoomedLayoutScheme, options, None)

  def reproject(zoomedLayoutScheme: ZoomedLayoutScheme, partitioner: Option[Partitioner]): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(zoomedLayoutScheme.crs, zoomedLayoutScheme, Options.DEFAULT, partitioner)

  def reproject(zoomedLayoutScheme: ZoomedLayoutScheme): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(zoomedLayoutScheme, Options.DEFAULT)

  def reproject(zoomedLayoutScheme: ZoomedLayoutScheme, bufferSize: Int, options: Options, partitioner: Option[Partitioner]): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(zoomedLayoutScheme.crs, zoomedLayoutScheme, bufferSize, options, partitioner)

  def reproject(zoomedLayoutScheme: ZoomedLayoutScheme, bufferSize: Int, options: Options): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(zoomedLayoutScheme.crs, zoomedLayoutScheme, bufferSize, options, None)

  def reproject(zoomedLayoutScheme: ZoomedLayoutScheme, bufferSize: Int, partitioner: Option[Partitioner]): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(zoomedLayoutScheme.crs, zoomedLayoutScheme, bufferSize, Options.DEFAULT, partitioner)

  def reproject(zoomedLayoutScheme: ZoomedLayoutScheme, bufferSize: Int): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(zoomedLayoutScheme, bufferSize, Options.DEFAULT, None)

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme, bufferSize: Int, options: Options, partitioner: Option[Partitioner]): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    TileRDDReproject(self, destCrs, Left(layoutScheme), bufferSize, options, partitioner)

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme, bufferSize: Int, options: Options): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(destCrs, layoutScheme, bufferSize, options, None)

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme, bufferSize: Int): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(destCrs, layoutScheme, bufferSize, Options.DEFAULT, None)

  def reproject(destCrs: CRS, layoutDefinition: LayoutDefinition, bufferSize: Int, options: Options, partitioner: Option[Partitioner]): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    TileRDDReproject(self, destCrs, Right(layoutDefinition), bufferSize, options, partitioner)

  def reproject(destCrs: CRS, layoutDefinition: LayoutDefinition, bufferSize: Int, options: Options): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    TileRDDReproject(self, destCrs, Right(layoutDefinition), bufferSize, options, None)

  def reproject(destCrs: CRS, layoutDefinition: LayoutDefinition, bufferSize: Int): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    TileRDDReproject(self, destCrs, Right(layoutDefinition), bufferSize, Options.DEFAULT, None)

  def reproject(destCrs: CRS, layoutDefinition: LayoutDefinition, options: Options): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    TileRDDReproject(self, destCrs, Right(layoutDefinition), options, None)

  def reproject(destCrs: CRS, layoutDefinition: LayoutDefinition, bufferSize: Int, partitioner: Option[Partitioner]): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    TileRDDReproject(self, destCrs, Right(layoutDefinition), bufferSize, Options.DEFAULT, partitioner)

  def reproject(destCrs: CRS, layoutDefinition: LayoutDefinition): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    TileRDDReproject(self, destCrs, Right(layoutDefinition), Options.DEFAULT, None)
}
