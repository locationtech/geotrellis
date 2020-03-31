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

package geotrellis.layer

import geotrellis.raster.{CellGrid, RasterSource, ResampleMethod}
import geotrellis.util._
import java.time.Instant

import geotrellis.raster.io.geotiff.OverviewStrategy
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.vector.io.json.CrsFormats


object Implicits extends Implicits

trait Implicits extends merge.Implicits
  with buffer.Implicits
  with CrsFormats
  with stitch.Implicits
  with mapalgebra.Implicits
  with mapalgebra.focal.Implicits
  with mapalgebra.focal.hillshade.Implicits
  with mapalgebra.local.Implicits
  with mapalgebra.local.temporal.Implicits
  with mask.Implicits {

  implicit def longToInstant(millis: Long): Instant = Instant.ofEpochMilli(millis)

  /** Necessary for Contains.forPoint query */
  implicit def tileLayerMetadataToMapKeyTransform[K](tm: TileLayerMetadata[K]): MapKeyTransform = tm.mapTransform

  implicit class WithContextCollectionWrapper[K, V, M](val seq: Seq[(K, V)] with Metadata[M]) {
    def withContext[K2, V2](f: Seq[(K, V)] => Seq[(K2, V2)]) =
      new ContextCollection(f(seq), seq.metadata)

    def mapContext[M2](f: M => M2) =
      new ContextCollection(seq, f(seq.metadata))
  }

  implicit class withTileLayerCollectionMethods[K: SpatialComponent](val self: TileLayerCollection[K])
    extends TileLayerCollectionMethods[K]

  implicit class withCellGridLayoutCollectionMethods[K: SpatialComponent, V <: CellGrid[Int], M: GetComponent[*, LayoutDefinition]](val self: Seq[(K, V)] with Metadata[M])
    extends CellGridLayoutCollectionMethods[K, V, M]

  implicit class TileToLayoutOps(val self: RasterSource) {
    def tileToLayout[K: SpatialComponent](
      layout: LayoutDefinition,
      tileKeyTransform: SpatialKey => K,
      resampleMethod: ResampleMethod = NearestNeighbor,
      strategy: OverviewStrategy = OverviewStrategy.DEFAULT
    ): LayoutTileSource[K] =
      LayoutTileSource(self.resampleToGrid(layout, resampleMethod, strategy), layout, tileKeyTransform)

    def tileToLayout(layout: LayoutDefinition, resampleMethod: ResampleMethod): LayoutTileSource[SpatialKey] =
      tileToLayout(layout, identity, resampleMethod)

    def tileToLayout(layout: LayoutDefinition): LayoutTileSource[SpatialKey] =
      tileToLayout(layout, NearestNeighbor)
  }
}
