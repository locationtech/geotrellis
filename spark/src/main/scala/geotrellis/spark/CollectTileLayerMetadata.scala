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

package geotrellis.spark

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark.ingest._
import geotrellis.spark.tiling._
import geotrellis.util._
import geotrellis.vector.{Extent, ProjectedExtent}
import cats.Functor
import cats.implicits._
import geotrellis.layers.TileLayerMetadata
import org.apache.spark.rdd._


object CollectTileLayerMetadata {
  private def collectMetadata[
    K: (? => TilerKeyMethods[K, K2]),
    V <: CellGrid[Int],
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)]): (Extent, CellType, CellSize, KeyBounds[K2]) = {
    rdd
      .map { case (key, grid) =>
        val extent = key.extent
        val boundsKey = key.translate(SpatialKey(0,0))
        // Bounds are return to set the non-spatial dimensions of the keybounds; the spatial keybounds are set outside this call.
        (extent, grid.cellType, CellSize(extent, grid.cols, grid.rows), KeyBounds(boundsKey, boundsKey))
      }
      .reduce { (tuple1, tuple2) =>
        val (extent1, cellType1, cellSize1, bounds1) = tuple1
        val (extent2, cellType2, cellSize2, bounds2) = tuple2
        (
          extent1.combine(extent2),
          cellType1.union(cellType2),
          if (cellSize1.resolution < cellSize2.resolution) cellSize1 else cellSize2,
          bounds1.combine(bounds2)
        )
      }
  }

  private def collectMetadataWithCRS[
    K: GetComponent[?, ProjectedExtent]: (? => TilerKeyMethods[K, K2]),
    V <: CellGrid[Int],
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)]): (Extent, CellType, CellSize, KeyBounds[K2], CRS) = {
    val (extent, cellType, cellSize, crsSet, bounds) =
      rdd
      .map { case (key, grid) =>
        val ProjectedExtent(extent, crs) = key.getComponent[ProjectedExtent]
        val boundsKey = key.translate(SpatialKey(0,0))
        // Bounds are return to set the non-spatial dimensions of the keybounds; the spatial keybounds are set outside this call.
        (extent, grid.cellType, CellSize(extent, grid.cols, grid.rows), Set(crs), KeyBounds(boundsKey, boundsKey))
      }
      .reduce { (tuple1, tuple2) =>
        val (extent1, cellType1, cellSize1, crs1, bounds1) = tuple1
        val (extent2, cellType2, cellSize2, crs2, bounds2) = tuple2
        (
          extent1.combine(extent2),
          cellType1.union(cellType2),
          if (cellSize1.resolution < cellSize2.resolution) cellSize1 else cellSize2,
          crs1 ++ crs2,
          bounds1.combine(bounds2)
          )
      }
    require(crsSet.size == 1, s"Multiple CRS tags found: $crsSet")
    (extent, cellType, cellSize, bounds, crsSet.head)
  }

  /**
    * Compose Extents from given raster tiles and fit it on given
    * TileLayout.
    */
  def fromRDD[
    K: (? => TilerKeyMethods[K, K2]),
    V <: CellGrid[Int],
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)], crs: CRS, layout: LayoutDefinition): TileLayerMetadata[K2] = {
    val (extent, cellType, _, bounds) = collectMetadata(rdd)
    val kb = bounds.setSpatialBounds(KeyBounds(layout.mapTransform(extent)))
    TileLayerMetadata(cellType, layout, extent, crs, kb)
  }

  /**
    * Compose Extents from given raster tiles and use LayoutScheme to
    * create the LayoutDefinition.
    */
  def fromRDD[
    K: (? => TilerKeyMethods[K, K2]) ,
    V <: CellGrid[Int],
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)], crs: CRS, scheme: LayoutScheme): (Int, TileLayerMetadata[K2]) = {
    val (extent, cellType, cellSize, bounds) = collectMetadata(rdd)
    val LayoutLevel(zoom, layout) = scheme.levelFor(extent, cellSize)
    val kb = bounds.setSpatialBounds(KeyBounds(layout.mapTransform(extent)))
    (zoom, TileLayerMetadata(cellType, layout, extent, crs, kb))
  }

  /**
    * Compose Extents from given raster tiles and use
    * [[geotrellis.spark.tiling.ZoomedLayoutScheme]] to create the
    * [[geotrellis.spark.tiling.LayoutDefinition]].
    */
  def fromRDD[
    K: (? => TilerKeyMethods[K, K2]) ,
    V <: CellGrid[Int],
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)], crs: CRS, scheme: ZoomedLayoutScheme):
    (Int, TileLayerMetadata[K2]) =
      _fromRDD[K, V, K2](rdd, crs, scheme, None)

  /**
    * Compose Extents from given raster tiles using
    * [[geotrellis.spark.tiling.ZoomedLayoutScheme]] and a maximum
    * zoom value.
    */
  def fromRDD[
    K: (? => TilerKeyMethods[K, K2]) ,
    V <: CellGrid[Int],
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)], crs: CRS, scheme: ZoomedLayoutScheme, maxZoom: Int):
    (Int, TileLayerMetadata[K2]) =
      _fromRDD[K, V, K2](rdd, crs, scheme, Some(maxZoom))

  private def _fromRDD[
    K: (? => TilerKeyMethods[K, K2]) ,
    V <: CellGrid[Int],
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)], crs: CRS, scheme: ZoomedLayoutScheme, maxZoom: Option[Int]):
    (Int, TileLayerMetadata[K2]) = {
      val (extent, cellType, cellSize, bounds) = collectMetadata(rdd)
      val LayoutLevel(zoom, layout) = maxZoom match {
        case Some(zoom) => scheme.levelForZoom(maxZoom.get)
        case _ => scheme.levelFor(extent, cellSize)
      }
    val kb = bounds.setSpatialBounds(KeyBounds(layout.mapTransform(extent)))
    (zoom, TileLayerMetadata(cellType, layout, extent, crs, kb))
  }

  def fromRDD[
    K: GetComponent[?, ProjectedExtent]: (? => TilerKeyMethods[K, K2]),
    V <: CellGrid[Int],
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)], scheme: LayoutScheme): (Int, TileLayerMetadata[K2]) = {
    val (extent, cellType, cellSize, bounds, crs) = collectMetadataWithCRS(rdd)
    val LayoutLevel(zoom, layout) = scheme.levelFor(extent, cellSize)
    val kb = bounds.setSpatialBounds(KeyBounds(layout.mapTransform(extent)))
    (zoom, TileLayerMetadata(cellType, layout, extent, crs, kb))
  }

  def fromRDD[
    K: GetComponent[?, ProjectedExtent]: (? => TilerKeyMethods[K, K2]),
    V <: CellGrid[Int],
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)],  scheme: ZoomedLayoutScheme):
    (Int, TileLayerMetadata[K2]) =
      _fromRDD[K, V, K2](rdd, scheme, None)

  def fromRDD[
    K: GetComponent[?, ProjectedExtent]: (? => TilerKeyMethods[K, K2]),
    V <: CellGrid[Int],
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)], scheme: ZoomedLayoutScheme, maxZoom: Int):
    (Int, TileLayerMetadata[K2]) =
      _fromRDD[K, V, K2](rdd, scheme, Some(maxZoom))

  private def _fromRDD[
    K: GetComponent[?, ProjectedExtent]: (? => TilerKeyMethods[K, K2]),
    V <: CellGrid[Int],
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)], scheme: ZoomedLayoutScheme, maxZoom: Option[Int]):
  (Int, TileLayerMetadata[K2]) = {
    val (extent, cellType, cellSize, bounds, crs) = collectMetadataWithCRS(rdd)
    val LayoutLevel(zoom, layout) = maxZoom match {
      case Some(zoom) => scheme.levelForZoom(maxZoom.get)
      case _ => scheme.levelFor(extent, cellSize)
    }
    val kb = bounds.setSpatialBounds(KeyBounds(layout.mapTransform(extent)))
    (zoom, TileLayerMetadata(cellType, layout, extent, crs, kb))
  }

  def fromRDD[
    K: GetComponent[?, ProjectedExtent]: (? => TilerKeyMethods[K, K2]),
    V <: CellGrid[Int],
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)], layoutDefinition: LayoutDefinition): TileLayerMetadata[K2] = {
    val (extent, cellType, cellSize, bounds, crs) = collectMetadataWithCRS(rdd)
    val kb = bounds.setSpatialBounds(KeyBounds(layoutDefinition.mapTransform(extent)))
    TileLayerMetadata(cellType, layoutDefinition, extent, crs, kb)
  }
}
