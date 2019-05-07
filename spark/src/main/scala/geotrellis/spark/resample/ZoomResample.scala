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

package geotrellis.spark.resample

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.util._
import geotrellis.vector.Extent

import org.apache.spark.rdd.RDD
import spire.implicits._

object ZoomResample {
  private def gridBoundsAtZoom(sourceZoom: Int, spatialKey: SpatialKey, targetZoom: Int): TileBounds = {
    val SpatialKey(col, row) = spatialKey
    val zoomDiff = targetZoom - sourceZoom
    val factor = math.pow(2, zoomDiff).toInt
    val (minCol, minRow) = (col * factor, row * factor)
    val (maxCol, maxRow) = (((col + 1) * factor) - 1, ((row + 1) * factor) - 1)
    GridBounds(minCol, minRow, maxCol, maxRow)
  }

  private def boundsAtZoom[K: SpatialComponent](sourceZoom: Int, bounds: Bounds[K], targetZoom: Int): Bounds[K] =
    bounds match {
      case KeyBounds(minKey, maxKey) =>
        val min = {
          val gb = gridBoundsAtZoom(sourceZoom, minKey.getComponent[SpatialKey], targetZoom)
          minKey.setComponent(SpatialKey(gb.colMin, gb.rowMin))
        }

        val max = {
          val gb = gridBoundsAtZoom(sourceZoom, maxKey.getComponent[SpatialKey], targetZoom)
          maxKey.setComponent(SpatialKey(gb.colMax, gb.rowMax))
        }
        KeyBounds(min, max)
      case EmptyBounds =>
        EmptyBounds
    }

  /** Resamples a tile layer from a lower zoom level to a higher zoom level.
    * The levels are based on the ZoomedLayoutScheme.
    *
    * @param       rdd              The RDD to be resampled.
    * @param       sourceZoom       The zoom level of the rdd.
    * @param       targetZoom       The zoom level we want to resample to.
    * @param       targetGridBounds Optionally, a grid bounds in the target zoom level we want to filter by.
    * @param       method           The resample method to use for resampling.
    */
  def apply[
    K: SpatialComponent,
    V <: CellGrid[Int]: (? => TileResampleMethods[V])
  ](
    rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    sourceZoom: Int,
    targetZoom: Int,
    targetGridBounds: Option[TileBounds] = None,
    method: ResampleMethod = NearestNeighbor
  ): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] = {
    require(sourceZoom < targetZoom, "This resample call requires that the target zoom level be greater than the source zoom level")
    val tileSize = rdd.metadata.layout.tileLayout.tileCols
    val targetLayoutDefinition =
      ZoomedLayoutScheme.layoutForZoom(targetZoom, rdd.metadata.layout.extent, tileSize)
    val targetMapTransform = targetLayoutDefinition.mapTransform
    val sourceMapTransform = rdd.metadata.mapTransform
    val (resampledRdd: RDD[(K, V)], md) =
      targetGridBounds match {
        case Some(tgb) =>
          val resampleKeyBounds: KeyBounds[K] =
            boundsAtZoom(sourceZoom, rdd.metadata.bounds, targetZoom).get

          resampleKeyBounds.toGridBounds.intersection(tgb) match {
            case Some(resampleGridBounds) => {
              val resampled: RDD[(K, V)] = rdd.flatMap { case (key, tile) =>
                val gbaz: Option[GridBounds[Int]] =
                  gridBoundsAtZoom(sourceZoom, key.getComponent[SpatialKey], targetZoom)
                    .intersection(resampleGridBounds)

                gbaz.map { gb =>
                  gb.coordsIter
                    .map { case (col, row) =>
                      val sourceExtent = sourceMapTransform.keyToExtent(key.getComponent[SpatialKey])
                      val targetExtent = targetMapTransform.keyToExtent(col, row)
                      val resampled = tile.resample(
                        sourceExtent,
                        RasterExtent(targetExtent, tileSize, tileSize),
                        method
                      )

                      (key.setComponent(SpatialKey(col, row)), resampled)
                    }
                }.getOrElse(Iterator.empty)
              }

              val extent: Extent =
                targetMapTransform(resampleGridBounds).intersection(rdd.metadata.extent).get

              val md = rdd.metadata.copy(
                layout = targetLayoutDefinition,
                bounds = resampleKeyBounds.setSpatialBounds(resampleGridBounds),
                extent = extent
              )

              (resampled, md)
            }
            case None => {
              val md = rdd.metadata.copy(
                layout = targetLayoutDefinition,
                bounds = (EmptyBounds: Bounds[K])
              )

              (rdd.sparkContext.emptyRDD[(K, V)], md)
            }
          }
        case None => {
          val resampled: RDD[(K, V)] =
            rdd
              .flatMap { case (key, tile) =>
                gridBoundsAtZoom(sourceZoom, key.getComponent[SpatialKey], targetZoom)
                  .coordsIter
                  .map { case (col, row) =>
                    val sourceExtent = sourceMapTransform.keyToExtent(key.getComponent[SpatialKey])
                    val targetExtent = targetMapTransform.keyToExtent(col, row)
                    val resampled =
                      tile.resample(sourceExtent, RasterExtent(targetExtent, tileSize, tileSize), method)
                    (key.setComponent(SpatialKey(col, row)), resampled)
                  }
               }

          val md = rdd.metadata.copy(
            layout = targetLayoutDefinition,
            bounds = boundsAtZoom(sourceZoom, rdd.metadata.bounds, targetZoom)
          )

          (resampled, md)
        }
      }

    ContextRDD(resampledRdd, md)
  }
}
