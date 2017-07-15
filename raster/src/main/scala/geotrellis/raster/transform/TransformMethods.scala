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

package geotrellis.raster.transform

import geotrellis.raster.CellGrid
import geotrellis.util.MethodExtensions

trait TransformMethods[T <: CellGrid] extends MethodExtensions[T] {
  def rotate90(n: Int): T
  def rotate180: T = rotate90(2)
  def rotate270: T = rotate90(3)
  def rotate360: T = rotate90(4)
  def flipVertical: T
  def flipHorizontal: T
}
