/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.regrid

import geotrellis.layers.Metadata
import org.apache.spark.rdd.RDD
import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.prototype._
import geotrellis.raster.stitch._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util._
import geotrellis.vector._

import scala.reflect._

object Regrid {

  private case class Interval[N : Ordering](start: N, end: N) {
    val ord = implicitly[Ordering[N]]
    assert(ord.compare(start, end) < 1, "Row/col intervals must begin before they end")

    // let the interval be start to end, inclusive
    def intersect(that: Interval[N]) = {
        Interval(if (ord.compare(this.start, that.start) > 0) this.start else that.start,
                 if (ord.compare(this.end, that.end) < 0) this.end else that.end)
    }
  }

  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: ClassTag: Stitcher: (? => CropMethods[V]),
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](layer: RDD[(K, V)] with Metadata[M], tileCols: Int, tileRows: Int): RDD[(K, V)] with Metadata[M] = {
    val md = layer.metadata
    val ld = md.getComponent[LayoutDefinition]

    if(ld.tileLayout.tileCols == tileCols && ld.tileLayout.tileRows == tileRows) {
      layer
    } else {
      val ceil = { x: Double => math.ceil(x).toInt }

      val tl = ld.tileLayout
      val oldEx = ld.extent
      val CellSize(cellW, cellH) = ld.cellSize

      val (oldW, oldH) = (tl.tileCols, tl.tileRows)
      val (newW, newH) = (tileCols, tileRows)
      val ntl = TileLayout(ceil(tl.layoutCols * oldW / newW.toDouble), ceil(tl.layoutRows * oldH / newH.toDouble), tileCols, tileRows)

      // Set up a new layout where the upper-left corner of the (0,0) spatial key
      // is preserved, and the right and bottom edges are are adjusted to cover
      // the new extent of the resized tiles
      val nld = LayoutDefinition(
        Extent(
          oldEx.xmin,
          oldEx.ymax - cellH * ntl.layoutRows * tileRows,
          oldEx.xmin + cellW * ntl.layoutCols * tileCols,
          oldEx.ymax),
        ntl)

      val bounds =
        md.getComponent[Bounds[K]] match {
          case KeyBounds(minKey, maxKey) =>
            val ulSK = minKey.getComponent[SpatialKey]
            val lrSK = maxKey.getComponent[SpatialKey]
            val pxXrange = Interval(ulSK._1 * oldW.toLong, (lrSK._1 + 1) * oldW.toLong - 1)
            val pxYrange = Interval(ulSK._2 * oldH.toLong, (lrSK._2 + 1) * oldH.toLong - 1)

            val newMinKey = SpatialKey((pxXrange.start / tileCols).toInt, (pxYrange.start / tileRows).toInt)
            val newMaxKey = SpatialKey((pxXrange.end / tileCols).toInt, (pxYrange.end / tileRows).toInt)

            KeyBounds(minKey.setComponent[SpatialKey](newMinKey), maxKey.setComponent[SpatialKey](newMaxKey))
          case EmptyBounds =>
            EmptyBounds
        }

      val newMd =
        md.setComponent[LayoutDefinition](nld)
          .setComponent[Bounds[K]](bounds)

      val tiled: RDD[(K, V)] =
        layer
          .flatMap{ case (key, oldTile) => {
            val SpatialKey(oldX, oldY) = key.getComponent[SpatialKey]

            val oldXstart = oldX.toLong * oldW.toLong
            val oldYstart = oldY.toLong * oldH.toLong
            val oldXrange = Interval(oldXstart, oldXstart + oldW - 1)
            val oldYrange = Interval(oldYstart, oldYstart + oldH - 1)

            for (
              x <- (oldXstart / tileCols).toInt to (oldXrange.end.toDouble / tileCols).toInt ;
              newXrange = {
                val newXstart = x * tileCols.toLong
                Interval(newXstart, newXstart + tileCols - 1)
              } ;
              y <- (oldYstart / tileRows).toInt to (oldYrange.end.toDouble / tileRows).toInt ;
              newYrange = {
                val newYstart = y * tileRows.toLong
                Interval(newYstart, newYstart + tileRows - 1)
              }
            ) yield {
              val newKey = key.setComponent[SpatialKey](SpatialKey(x, y))

              val xSpan: Interval[Long] = oldXrange intersect newXrange
              val ySpan: Interval[Long] = oldYrange intersect newYrange
              newKey ->
                (oldTile.crop((xSpan.start - oldXstart).toInt,
                              (ySpan.start - oldYstart).toInt,
                              (xSpan.end - oldXstart).toInt,
                              (ySpan.end - oldYstart).toInt),
                 ((xSpan.start - newXrange.start).toInt, (ySpan.start - newYrange.start).toInt)
                )
            }
          }}
          .groupByKey
          .mapValues { tiles => implicitly[Stitcher[V]].stitch(tiles, tileCols, tileRows) }

      ContextRDD(tiled, newMd)
    }
  }

  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid[Int]: ClassTag: Stitcher: (? => CropMethods[V]),
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](layer: RDD[(K, V)] with Metadata[M], tileSize: Int): RDD[(K, V)] with Metadata[M] = apply(layer, tileSize, tileSize)

}
