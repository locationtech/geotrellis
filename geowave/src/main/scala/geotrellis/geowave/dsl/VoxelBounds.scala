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

import geotrellis.raster.GridBounds

/** VoxelBounds that we'll use for the internal API that allows to abstract over bounds dimensions; Doxel Bounds? */
trait VoxelBounds { self =>
  def colMin: Int
  def colMax: Int
  def rowMin: Int
  def rowMax: Int
  def toGridBounds: GridBounds[Int] = GridBounds[Int](colMin, rowMin, colMax, rowMax)
}
