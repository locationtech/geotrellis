/*
 * Copyright 2020 Azavea
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

package geotrellis.geowave.dsl

import geotrellis.raster.Dimensions
import geotrellis.geowave.dsl.json._
import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
sealed trait VoxelDimensions { self =>
  def width: Int
  def height: Int
  def toVoxelBounds: VoxelBounds
  def toDimensions: Dimensions[Int] = Dimensions[Int](width, height)
  def withTilingBounds(tb: TilingBounds): VoxelDimensions
}

@ConfiguredJsonCodec
case class VoxelDimensions2D(width: Int, height: Int) extends VoxelDimensions {
  def toVoxelBounds: VoxelBounds2D = VoxelBounds2D(0, width, 0, height)
  def withTilingBounds(tb: TilingBounds): VoxelDimensions2D =
    VoxelDimensions2D(tb.width.getOrElse(width), tb.height.getOrElse(height))
}
@ConfiguredJsonCodec
case class VoxelDimensions3D(width: Int, height: Int, depth: Int) extends VoxelDimensions {
  def toVoxelBounds: VoxelBounds3D = VoxelBounds3D(0, width, 0, height, 0, depth)
  def withTilingBounds(tb: TilingBounds): VoxelDimensions3D =
    VoxelDimensions3D(tb.width.getOrElse(width), tb.height.getOrElse(height), tb.depth.getOrElse(depth))
}
@ConfiguredJsonCodec
case class VoxelDimensions4D(width: Int, height: Int, depth: Int, spissitude: Int) extends VoxelDimensions {
  def toVoxelBounds: VoxelBounds4D = VoxelBounds4D(0, width, 0, height, 0, depth, 0, spissitude)
  def withTilingBounds(tb: TilingBounds): VoxelDimensions4D =
    VoxelDimensions4D(tb.width.getOrElse(width), tb.height.getOrElse(height), tb.depth.getOrElse(depth), tb.spissitude.getOrElse(spissitude))
}
