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
import geotrellis.vector.Extent

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

  /** Resample base layer to generate the next level "up" in the pyramid.
    *
    * When building the next pyramid level each tile is resampled individually, without sampling pixels from neighboring tiles.
    * In the common case of [[ZoomedLayoutScheme]], used when building a power of two pyramid, this means that
    * each resampled pixel is going to be equidistant to four pixels in the base layer.
    *
    * @param rdd           the base layer to be resampled
    * @param layoutScheme  the scheme used to generate next pyramid level
    * @param zoom          the pyramid or zoom level of base layer
    * @param options       the options for the pyramid process
    * @tparam K            RDD key type (ex: SpatialKey)
    * @tparam V            RDD value type (ex: Tile or MultibandTile)
    * @tparam M            Metadata associated with the RDD[(K,V)]
    */
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
          val extent: Extent = key.getComponent[SpatialKey].extent(sourceLayout)
          val newSpatialKey = nextLayout.mapTransform(extent.center)
          // Resample the tile on the map side of the pyramid step.
          // This helps with shuffle size.
          val resampled = tile.prototype(nextLayout.tileLayout.tileCols, nextLayout.tileLayout.tileRows)
          resampled.merge(extent, extent, tile, resampleMethod)

          (key.setComponent(newSpatialKey), (key, resampled))
        }

        partitioner
          .fold(transformedRdd.combineByKey(createTiles, mergeTiles1, mergeTiles2))(transformedRdd.combineByKey(createTiles _, mergeTiles1 _, mergeTiles2 _, _))
          .mapPartitions ( partition => partition.map { case (newKey: K, seq: Seq[(K, V)]) =>
            val newExtent = newKey.getComponent[SpatialKey].extent(nextLayout)
            val newTile = seq.head._2.prototype(nextLayout.tileLayout.tileCols, nextLayout.tileLayout.tileRows)

            for ((oldKey, tile) <- seq) {
              val oldExtent = oldKey.getComponent[SpatialKey].extent(sourceLayout)
              newTile.merge(newExtent, oldExtent, tile, NearestNeighbor)
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

  /** Produce all pyramid levels from start and end zoom.
    *
    * The first entry in the result stream is the tuple of `rdd` and `startZoom`.
    * The RDDs of pyramid levels have a recursive dependency on their base RDD.
    * Because RDDs are lazy take care when consuming this stream.
    * Choose to either persist the base layer or trigger jobs
    * in order to maximize the caching provided by the Spark BlockManager.
    *
    * @param rdd           the base layer to be resampled
    * @param layoutScheme  the scheme used to generate next pyramid level
    * @param startZoom     the pyramid or zoom level of base layer
    * @param endZoom       the pyramid or zoom level to stop pyramid process
    * @param options       the options for the pyramid process
    * @tparam K            RDD key type (ex: SpatialKey)
    * @tparam V            RDD value type (ex: Tile or MultibandTile)
    * @tparam M            Metadata associated with the RDD[(K,V)]
    *
    * @see [up]
    */
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
