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

package geotrellis.raster.viewshed

import geotrellis.raster._
import geotrellis.util.MethodExtensions


trait ViewshedMethods extends MethodExtensions[Tile] {
  def viewshed(col: Int, row: Int, exact: Boolean = false): Tile =
    if (exact)
      Viewshed(self, col, row)
    else
      ApproxViewshed(self, col, row)

  def viewshedOffsets(col: Int, row: Int, exact: Boolean = false): Tile =
    if (exact)
      Viewshed.offsets(self, col, row)
    else
      ApproxViewshed.offsets(self, col, row)
}
