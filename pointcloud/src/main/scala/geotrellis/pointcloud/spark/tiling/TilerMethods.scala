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

package geotrellis.pointcloud.spark.tiling

import io.pdal._
import geotrellis.spark._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd._

class TilerMethods(val self: RDD[PointCloud]) extends MethodExtensions[RDD[PointCloud]] {
  def tileToLayout(layoutDefinition: LayoutDefinition): RDD[(SpatialKey, PointCloud)] with Metadata[LayoutDefinition] =
    CutPointCloud(self, layoutDefinition)
}
