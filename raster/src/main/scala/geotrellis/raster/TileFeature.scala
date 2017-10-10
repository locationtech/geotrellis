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

/**
  * The TileFeature Type.  This is used for packaging a CellGrid (a
  * Tile or MultibandTile) together with some metadata.
  *
  * @param  tile  The CellGrid-derived tile
  * @param  data  The additional metadata
  */
case class TileFeature[+T <: CellGrid, D](tile: T, data: D) extends CellGrid {
  def cellType: CellType = tile.cellType
  def cols: Int = tile.cols
  def rows: Int = tile.rows
}

object TileFeature {
  implicit def tileFeatureToCellGrid[T <: CellGrid, D](tf: TileFeature[T, D]): T = tf.tile
}
