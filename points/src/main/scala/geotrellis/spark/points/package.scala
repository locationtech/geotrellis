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

package geotrellis.spark

import geotrellis.spark.tiling.TilerKeyMethods
import geotrellis.util._

package object points {
  implicit class withProjectedExtent3DTilerKeyMethods[K: Component[?, ProjectedExtent3D]](val self: K) extends TilerKeyMethods[K, SpatialKey] {
    def extent = self.getComponent[ProjectedExtent3D].extent3d.toExtent
    def translate(spatialKey: SpatialKey) = spatialKey
  }
}
