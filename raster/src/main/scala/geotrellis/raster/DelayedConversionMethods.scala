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

package geotrellis.raster

import geotrellis.util.MethodExtensions

trait DelayedConversionTileMethods extends MethodExtensions[Tile] {
  /** Delays the conversion of this tile's cell type until
    * it produces another Tile.
    *
    * @param      cellType       The target cell type of a future map or combine operation.
    *
    * @note This only has an affect for the result tiles of a combine or map operation.
    *       This will always produce an ArrayTile.
    */
  def delayedConversion(cellType: CellType): DelayedConversionTile =
    new DelayedConversionTile(self, cellType)
}

trait DelayedConversionMultibandTileMethods extends MethodExtensions[MultibandTile] {
  /** Delays the conversion of this tile's cell type until
    * it produces another Tile or MulitbandTile.
    *
    * @param      cellType       The target cell type of a future map or combine operation.
    *
    * @note This only has an affect for the result tiles of a combine or map operation.
    *       This will always produce an ArrayMultibandTile.
    */
  def delayedConversion(cellType: CellType): DelayedConversionMultibandTile =
    new DelayedConversionMultibandTile(self, cellType)
}
