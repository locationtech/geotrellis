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

import geotrellis.tiling._
import geotrellis.layers.{LayerId, Metadata}
import geotrellis.layers.avro._
import geotrellis.layers.index.KeyIndexMethod
import geotrellis.layers.json._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.resample._
import geotrellis.raster.prototype._
import geotrellis.util._
import geotrellis.vector.Extent

import com.typesafe.scalalogging.LazyLogging

import org.apache.spark.Partitioner
import org.apache.spark.rdd._
import org.apache.spark.storage.StorageLevel
import spray.json._

import scala.reflect.ClassTag

case class Pyramid[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TilePrototypeMethods[V]: ? => TileMergeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
](levels: Map[Int, RDD[(K, V)] with Metadata[M]]) {
  def apply(level: Int): RDD[(K, V)] with Metadata[M] = levels(level)

  def level(l: Int): RDD[(K, V)] with Metadata[M] = levels(l)

  def lookup(zoom: Int, key: K): Seq[V] = levels(zoom).lookup(key)

  def minZoom = levels.keys.min
  def maxZoom = levels.keys.max

  def persist(storageLevel: StorageLevel) = levels.mapValues{ _.persist(storageLevel) }

  def write(
    layerName: String,
    writer: LayerWriter[LayerId],
    keyIndexMethod: KeyIndexMethod[K]
  )(
    implicit arcK: AvroRecordCodec[K],
    jsfK: JsonFormat[K],
    arcV: AvroRecordCodec[V],
    jsfM: JsonFormat[M]
  ) = {
    for (z <- maxZoom to minZoom by -1) {
      writer.write[K, V, M](LayerId(layerName, z), levels(z), keyIndexMethod)
    }
  }

}

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

  def fromLayerReader[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag: SpatialComponent,
    V <: CellGrid[Int]: ? => TilePrototypeMethods[V]: ? => TileMergeMethods[V]: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]: Component[?, LayoutDefinition]
  ](layerName: String, layerReader: LayerReader[LayerId], maxZoom: Option[Int] = None, minZoom: Option[Int] = None): Pyramid[K, V, M] = {
    val zooms = layerReader.attributeStore.availableZoomLevels(layerName)

    val maxZoomLevel = maxZoom match {
      case Some(z) =>
        if (z > zooms.max)
          throw new IllegalArgumentException(s"Requested max zoom of $z is greater than max available zoom of ${zooms.max}")
        else z
      case None => zooms.max
    }
    val minZoomLevel = maxZoom match {
      case Some(z) =>
        if (z < zooms.min)
          throw new IllegalArgumentException(s"Requested min zoom of $z is greater than min available zoom of ${zooms.min}")
        else z
      case None => zooms.min
    }

    val seq = for (z <- maxZoomLevel to minZoomLevel by -1) yield {
      (z, layerReader.read[K, V, M](LayerId(layerName, z)))
    }
    Pyramid[K, V, M](seq.toMap)
  }

  def fromLayerRDD[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TilePrototypeMethods[V]: ? => TileMergeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    thisZoom: Option[Int] = None,
    endZoom: Option[Int] = None,
    resampleMethod: ResampleMethod = Average,
    partitioner: Option[Partitioner] = None
  ): Pyramid[K, V, M] = {
    val opts = Options(resampleMethod, partitioner)
    val gridBounds = rdd.metadata.getComponent[Bounds[K]] match {
      case kb: KeyBounds[K] => kb.toGridBounds
      case _ => throw new IllegalArgumentException("Cannot construct a pyramid for an empty layer")
    }
    val maxDim = math.max(gridBounds.width, gridBounds.height).toDouble
    val levels = math.ceil(math.log(maxDim)/math.log(2)).toInt

    (thisZoom, endZoom) match {
      case (None, None) => Pyramid(levelStream(rdd, new LocalLayoutScheme, levels, 0, opts).toMap)
      case (Some(start), None) => Pyramid(levelStream(rdd, new LocalLayoutScheme, start, math.max(0, start - levels), opts).toMap)
      case (None, Some(end)) => Pyramid(levelStream(rdd, new LocalLayoutScheme, end + levels, end, opts).toMap)
      case (Some(start), Some(end)) => Pyramid(levelStream(rdd, new LocalLayoutScheme, start, end, opts).toMap)
    }
  }

  /** Resample base layer to generate the next level "up" in the pyramid.
    *
    * Builds the pyramid level with a cell size twice that of the input level---the "next level up" in the pyramid.
    * Each tile is resampled individually, without sampling pixels from neighboring tiles to speed up the process.
    * The algorithm proceeds by reducing the input tiles by half using a resampling method over 2x2 pixel neighborhoods.
    * We support all [[AggregateResampleMethod]]s as well as NearestNeighbor and Bilinear resampling.  Nearest neighbor
    * resampling is, strictly speaking, non-deterministic in this setting, but is included to support categorical layers
    * (e.g., NLCD).  Given this method, input tile layers are obviously expected to comprise tiles with even pixel
    * dimensions.
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
    V <: CellGrid[Int]: ClassTag: ? => TilePrototypeMethods[V]: ? => TileMergeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    zoom: Int,
    options: Options
  ): (Int, RDD[(K, V)] with Metadata[M]) = {
    val Options(resampleMethod, partitioner) = options

    assert(!Seq(CubicConvolution, CubicSpline, Lanczos).contains(resampleMethod),
           s"${resampleMethod} resample method is not supported for pyramid construction")

    val sourceLayout = rdd.metadata.getComponent[LayoutDefinition]
    val sourceBounds = rdd.metadata.getComponent[Bounds[K]]
    val LayoutLevel(nextZoom, nextLayout) = layoutScheme.zoomOut(LayoutLevel(zoom, sourceLayout))

    assert(sourceLayout.tileCols % 2 == 0 && sourceLayout.tileRows % 2 == 0,
           s"Pyramid operation requires tiles with even dimensions, got ${sourceLayout.tileCols} x ${sourceLayout.tileRows}")

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
    def createTiles(tile: Raster[V]): Seq[Raster[V]]                                = Seq(tile)
    def mergeTiles1(tiles: Seq[Raster[V]], tile: Raster[V]): Seq[Raster[V]]         = tiles :+ tile
    def mergeTiles2(tiles1: Seq[Raster[V]], tiles2: Seq[Raster[V]]): Seq[Raster[V]] = tiles1 ++ tiles2

    val nextRdd = {
      val transformedRdd = rdd
        .map { case (key, tile) =>
          val extent: Extent = key.getComponent[SpatialKey].extent(sourceLayout)
          val newSpatialKey = nextLayout.mapTransform(extent.center)

          // Resample the tile on the map side of the pyramid step.
          // This helps with shuffle size.
          val resampled = tile.prototype(sourceLayout.tileCols / 2, sourceLayout.tileRows / 2)
          resampled.merge(extent, extent, tile, resampleMethod)

          (key.setComponent(newSpatialKey), Raster(resampled, extent))
        }

      partitioner
        .fold(transformedRdd.combineByKey(createTiles, mergeTiles1, mergeTiles2))(transformedRdd.combineByKey(createTiles _, mergeTiles1 _, mergeTiles2 _, _))
        .mapPartitions ( partition => partition.map { case (newKey: K, seq: Seq[Raster[V]]) =>
           val newExtent = newKey.getComponent[SpatialKey].extent(nextLayout)
           val newTile = seq.head.tile.prototype(nextLayout.tileLayout.tileCols, nextLayout.tileLayout.tileRows)

           for (raster <- seq) {
             newTile.merge(newExtent, raster.extent, raster.tile, NearestNeighbor)
           }
           (newKey, newTile: V)
        },  preservesPartitioning = true)
    }

    nextZoom -> new ContextRDD(nextRdd, nextMetadata)
  }

  def up[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
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
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
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
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    startZoom: Int,
    endZoom: Int
  ): Stream[(Int, RDD[(K, V)] with Metadata[M])] =
    levelStream(rdd, layoutScheme, startZoom, endZoom, Options.DEFAULT)

  def levelStream[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    startZoom: Int,
    options: Options
  ): Stream[(Int, RDD[(K, V)] with Metadata[M])] =
    levelStream(rdd, layoutScheme, startZoom, 0, options)

  def levelStream[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    startZoom: Int
  ): Stream[(Int, RDD[(K, V)] with Metadata[M])] =
    levelStream(rdd, layoutScheme, startZoom, Options.DEFAULT)

  def upLevels[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
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
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    startZoom: Int,
    endZoom: Int
  )(f: (RDD[(K, V)] with Metadata[M], Int) => Unit): RDD[(K, V)] with Metadata[M] =
    upLevels(rdd, layoutScheme, startZoom, endZoom, Options.DEFAULT)(f)

  def upLevels[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    startZoom: Int,
    options: Options
  )(f: (RDD[(K, V)] with Metadata[M], Int) => Unit): RDD[(K, V)] with Metadata[M] =
    upLevels(rdd, layoutScheme, startZoom, 0, options)(f)

  def upLevels[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M],
    layoutScheme: LayoutScheme,
    startZoom: Int
  )(f: (RDD[(K, V)] with Metadata[M], Int) => Unit): RDD[(K, V)] with Metadata[M] =
    upLevels(rdd, layoutScheme, startZoom, Options.DEFAULT)(f)
}
