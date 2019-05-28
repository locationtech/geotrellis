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

package geotrellis.spark.pipeline.ast

import geotrellis.layers.{Metadata, TileLayerMetadata}
import geotrellis.raster.crop.CropMethods
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.raster.reproject.Reproject.{Options => RasterReprojectOptions}
import geotrellis.raster.reproject.{RasterRegionReproject, TileReprojectMethods}
import geotrellis.raster.stitch.Stitcher
import geotrellis.raster.CellGrid
import geotrellis.raster.resample.ResampleMethod
import geotrellis.tiling._
import geotrellis.layers.io.avro.AvroRecordCodec
import geotrellis.spark.tiling.TilerKeyMethods
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.pipeline.json.transform.{Pyramid => JsonPyramid}
import geotrellis.spark.pipeline.json.transform._
import geotrellis.spark.pyramid.Pyramid
import geotrellis.vector.ProjectedExtent
import geotrellis.util._
import org.apache.spark.HashPartitioner
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

trait Transform[F, T] extends Node[T]

object Transform {
  def perTileReproject[
    I: Component[?, ProjectedExtent],
    V <: CellGrid[Int]: (? => TileReprojectMethods[V])
  ](arg: Reproject)(rdd: RDD[(I, V)]): RDD[(I, V)] = {
    (arg.scheme, arg.maxZoom) match {
      case (Left(layoutScheme: ZoomedLayoutScheme), Some(mz)) =>
        val LayoutLevel(_, layoutDefinition) = layoutScheme.levelForZoom(mz)
        rdd.reproject(arg.getCRS, RasterReprojectOptions(method = arg.resampleMethod, targetCellSize = Some(layoutDefinition.cellSize)))

      case _ => rdd.reproject(arg.getCRS)
    }
  }

  def bufferedReproject[
    K: SpatialComponent: Boundable: ClassTag,
    V <: CellGrid[Int]: ClassTag: RasterRegionReproject: Stitcher: (? => TileReprojectMethods[V]): (? => CropMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
  ](arg: Reproject)(rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] = {
    (arg.scheme, arg.maxZoom) match {
      case (Left(layoutScheme: ZoomedLayoutScheme), Some(mz)) =>
        val LayoutLevel(zoom, layoutDefinition) = layoutScheme.levelForZoom(mz)
        rdd.reproject(arg.getCRS, layoutDefinition, RasterReprojectOptions(method = arg.resampleMethod, targetCellSize = Some(layoutDefinition.cellSize)))._2

      case (Left(layoutScheme), _) =>
        rdd.reproject(arg.getCRS, layoutScheme, arg.resampleMethod)._2

      case (Right(layoutDefinition), _) =>
        rdd.reproject(arg.getCRS, layoutDefinition, arg.resampleMethod)._2
    }
  }

  def tileToLayout[
    K: Boundable: SpatialComponent: ClassTag,
    I: Component[?, ProjectedExtent]: ? => TilerKeyMethods[I, K],
    V <: CellGrid[Int]: (? => TileReprojectMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V]): ClassTag
  ](arg: TileToLayout)(rdd: RDD[(I, V)]): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] = {
    val md = { // collecting floating metadata allows detecting upsampling
      val (_, md) = rdd.collectMetadata(FloatingLayoutScheme(arg.tileSize.getOrElse(256)))
      md.copy(cellType = arg.cellType.getOrElse(md.cellType))
    }
    withTilerMethods(rdd).tileToLayout[K](md, arg.resampleMethod)
  }

  def retileToLayoutSpatial[
    V <: CellGrid[Int]: (? => TileReprojectMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V]): ClassTag
  ](arg: RetileToLayout)(rdd: RDD[(SpatialKey, V)] with Metadata[TileLayerMetadata[SpatialKey]]): RDD[(SpatialKey, V)] with Metadata[TileLayerMetadata[SpatialKey]] = {
    val md = rdd.metadata
    val mapKeyTransform = md.mapTransform
    val projectedRDD =  rdd.map { x => (ProjectedExtent(mapKeyTransform(x._1), rdd.metadata.crs), x._2) }
    val bounds: KeyBounds[SpatialKey] = rdd.metadata.bounds.get
    val spatialBounds = KeyBounds(mapKeyTransform(rdd.metadata.extent))
    val retiledLayerMetadata: TileLayerMetadata[SpatialKey] = rdd.metadata.copy(
      layout = arg.layoutDefinition,
      bounds = KeyBounds(
        minKey = bounds.minKey.setComponent[SpatialKey](spatialBounds.minKey),
        maxKey = bounds.maxKey.setComponent[SpatialKey](spatialBounds.maxKey)
      )
    )

    projectedRDD.tileToLayout(retiledLayerMetadata, arg.resampleMethod)
  }

  def retileToLayoutTemporal[
    V <: CellGrid[Int]: (? => TileReprojectMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V]): ClassTag
  ](arg: RetileToLayout)(rdd: RDD[(SpaceTimeKey, V)] with Metadata[TileLayerMetadata[SpaceTimeKey]]): RDD[(SpaceTimeKey, V)] with Metadata[TileLayerMetadata[SpaceTimeKey]] = {
    val md = rdd.metadata
    val mapKeyTransform = md.mapTransform
    val projectedRDD = rdd.map { x => (TemporalProjectedExtent(mapKeyTransform(x._1), rdd.metadata.crs, x._1.getComponent[TemporalKey].instant), x._2) }
    val bounds: KeyBounds[SpaceTimeKey] = rdd.metadata.bounds.get
    val spatialBounds = KeyBounds(mapKeyTransform(rdd.metadata.extent))
    val retiledLayerMetadata: TileLayerMetadata[SpaceTimeKey] = rdd.metadata.copy(
      layout = arg.layoutDefinition,
      bounds = KeyBounds(
        minKey = bounds.minKey.setComponent[SpatialKey](spatialBounds.minKey),
        maxKey = bounds.maxKey.setComponent[SpatialKey](spatialBounds.maxKey)
      )
    )

    projectedRDD.tileToLayout(retiledLayerMetadata, arg.resampleMethod)
  }

  def pyramid[
    K: SpatialComponent : AvroRecordCodec : JsonFormat : ClassTag,
    V <: CellGrid[Int] : AvroRecordCodec : ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
  ](arg: JsonPyramid)(rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]): Stream[(Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]])] = {
    def pyramid(resampleMethod: ResampleMethod): Stream[(Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]])] = {
      require(!rdd.metadata.bounds.isEmpty, "Can not pyramid an empty RDD")
      val part = rdd.partitioner.getOrElse(new HashPartitioner(rdd.partitions.length))
      val (baseZoom, scheme) =
        arg.startZoom match {
          case Some(zoom) =>
            zoom -> ZoomedLayoutScheme(rdd.metadata.crs, rdd.metadata.tileRows)

          case None =>
            val zoom = LocalLayoutScheme.inferLayoutLevel(rdd.metadata.layout)
            zoom -> new LocalLayoutScheme
        }

      Pyramid.levelStream(
        rdd, scheme, baseZoom, arg.endZoom.getOrElse(0),
        Pyramid.Options(resampleMethod=resampleMethod, partitioner=part)
      )
    }

    pyramid(arg.resampleMethod)
  }
}
