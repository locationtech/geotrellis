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

package geotrellis.spark.pyramid

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.resample._
import geotrellis.raster.prototype._
import geotrellis.util._

import org.apache.spark.Partitioner
import org.apache.spark.rdd._

import scala.reflect.ClassTag

object Pyramid extends LazyLogging {
  case class Options(
    resampleMethod: ResampleMethod = NearestNeighbor,
    partitioner: Option[Partitioner] = None
  )
  object Options {
    def DEFAULT = Options()
    implicit def partitionerToOptions(p: Partitioner): Options = Options(partitioner = Some(p))
    implicit def optPartitionerToOptions(p: Option[Partitioner]): Options = Options(partitioner = p)
    implicit def methodToOptions(m: ResampleMethod): Options = Options(resampleMethod = m)
  }

  def up[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    zoom: Int,
    options: Options
  ): (Int, RDD[(K, V)] with Metadata[M]) = {
    val Options(resampleMethod, partitioner) = options

    val sourceLayout = rdd.metadata.getComponent[LayoutDefinition]
    val sourceBounds = rdd.metadata.getComponent[Bounds[K]]
    val LayoutLevel(nextZoom, nextLayout) = layoutScheme.zoomOut(LayoutLevel(zoom, sourceLayout))

    val nextKeyBounds =
      sourceBounds match {
        case EmptyBounds => EmptyBounds
        case kb: KeyBounds[K] =>
          // If we treat previous layout as extent and next layout as tile layout we are able to hijack MapKeyTransform
          // to translate the spatial component of source KeyBounds to next KeyBounds
          val extent = sourceLayout.extent
          val sourceRe = RasterExtent(extent, sourceLayout.layoutCols, sourceLayout.layoutRows)
          val targetRe = RasterExtent(extent, nextLayout.layoutCols, nextLayout.layoutRows)
          val SpatialKey(sourceColMin, sourceRowMin) = kb.minKey.getComponent[SpatialKey]
          val SpatialKey(sourceColMax, sourceRowMax) = kb.maxKey.getComponent[SpatialKey]
          val (colMin, rowMin) = {
            val (x, y) = sourceRe.gridToMap(sourceColMin, sourceRowMin)
            targetRe.mapToGrid(x, y)
          }

          val (colMax, rowMax) = {
            val (x, y) = sourceRe.gridToMap(sourceColMax, sourceRowMax)
            targetRe.mapToGrid(x, y)
          }

          KeyBounds(
            kb.minKey.setComponent(SpatialKey(colMin, rowMin)),
            kb.maxKey.setComponent(SpatialKey(colMax, rowMax)))
      }

    val nextMetadata =
      rdd.metadata
        .setComponent(nextLayout)
        .setComponent(nextKeyBounds)

    // Functions for combine step
    def createTiles(tile: (K, V)): Seq[(K, V)]                             = Seq(tile)
    def mergeTiles1(tiles: Seq[(K, V)], tile: (K, V)): Seq[(K, V)]         = tiles :+ tile
    def mergeTiles2(tiles1: Seq[(K, V)], tiles2: Seq[(K, V)]): Seq[(K, V)] = tiles1 ++ tiles2

    val nextRdd = {
     val transformedRdd = rdd
        .map { case (key, tile) =>
          val extent = sourceLayout.mapTransform(key)
          val newSpatialKey = nextLayout.mapTransform(extent.center)
          (key.setComponent(newSpatialKey), (key, tile))
        }

        partitioner
          .fold(transformedRdd.combineByKey(createTiles, mergeTiles1, mergeTiles2))(transformedRdd.combineByKey(createTiles _, mergeTiles1 _, mergeTiles2 _, _))
          .mapPartitions ( partition => partition.map { case (newKey: K, seq: Seq[(K, V)]) =>
            val newExtent = nextLayout.mapTransform(newKey)
            val newTile = seq.head._2.prototype(nextLayout.tileLayout.tileCols, nextLayout.tileLayout.tileRows)

            for ((oldKey, tile) <- seq) {
              val oldExtent = sourceLayout.mapTransform(oldKey)
              newTile.merge(newExtent, oldExtent, tile, resampleMethod)
            }
            (newKey, newTile: V)
          },  preservesPartitioning = true)
    }

    nextZoom -> new ContextRDD(nextRdd, nextMetadata)
  }

  def up[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    zoom: Int
  ): (Int, RDD[(K, V)] with Metadata[M]) =
    up(rdd, layoutScheme, zoom, Options.DEFAULT)

  def levelStream[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    startZoom: Int,
    endZoom: Int,
    options: Options
  ): Stream[(Int, RDD[(K, V)] with Metadata[M])] =
    (startZoom, rdd) #:: {
      if (startZoom > endZoom) {
        val (nextZoom, nextRdd) = Pyramid.up(rdd, layoutScheme, startZoom, options)
        levelStream(nextRdd, layoutScheme, nextZoom, endZoom, options)
      } else {
        Stream.empty
      }
    }

  def levelStream[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    startZoom: Int,
    endZoom: Int
  ): Stream[(Int, RDD[(K, V)] with Metadata[M])] =
    levelStream(rdd, layoutScheme, startZoom, endZoom, Options.DEFAULT)

  def levelStream[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    startZoom: Int,
    options: Options
  ): Stream[(Int, RDD[(K, V)] with Metadata[M])] =
    levelStream(rdd, layoutScheme, startZoom, 0, options)

  def levelStream[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    startZoom: Int
  ): Stream[(Int, RDD[(K, V)] with Metadata[M])] =
    levelStream(rdd, layoutScheme, startZoom, Options.DEFAULT)

  def upLevels[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    startZoom: Int,
    endZoom: Int,
    options: Options
  )(f: (RDD[(K, V)] with Metadata[M], Int) => Unit): RDD[(K, V)] with Metadata[M] = {
    val Options(resampleMethod, partitioner) = options

    def runLevel(thisRdd: RDD[(K, V)] with Metadata[M], thisZoom: Int): (RDD[(K, V)] with Metadata[M], Int) =
      if (thisZoom > endZoom) {
        f(thisRdd, thisZoom)
        val (nextZoom, nextRdd) = Pyramid.up(thisRdd, layoutScheme, thisZoom, options)
        runLevel(nextRdd, nextZoom)
      } else {
        f(thisRdd, thisZoom)
        (thisRdd, thisZoom)
      }

    runLevel(rdd, startZoom)._1
  }

  def upLevels[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    startZoom: Int,
    endZoom: Int
  )(f: (RDD[(K, V)] with Metadata[M], Int) => Unit): RDD[(K, V)] with Metadata[M] =
    upLevels(rdd, layoutScheme, startZoom, endZoom, Options.DEFAULT)(f)

  def upLevels[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    startZoom: Int,
    options: Options
  )(f: (RDD[(K, V)] with Metadata[M], Int) => Unit): RDD[(K, V)] with Metadata[M] =
    upLevels(rdd, layoutScheme, startZoom, 0, options)(f)

  def upLevels[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    startZoom: Int
  )(f: (RDD[(K, V)] with Metadata[M], Int) => Unit): RDD[(K, V)] with Metadata[M] =
    upLevels(rdd, layoutScheme, startZoom, Options.DEFAULT)(f)
}
