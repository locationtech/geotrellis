/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.io.index

import geotrellis.spark._
import geotrellis.spark.io.index.geowave._

private[index] trait GeowaveKeyIndexMethod

object GeowaveKeyIndexMethod extends GeowaveKeyIndexMethod {

  implicit def spatialKeyIndexMethod(m: GeowaveKeyIndexMethod): KeyIndexMethod[SpatialKey] =
    new KeyIndexMethod[SpatialKey] {
      def createIndex(keyBounds: KeyBounds[SpatialKey]): KeyIndex[SpatialKey] = {
        val xBits = resolution(keyBounds.maxKey.col, keyBounds.minKey.col)
        val yBits = resolution(keyBounds.maxKey.row, keyBounds.minKey.row)
        new GeowaveSpatialKeyIndex(keyBounds, xBits, yBits)
      }
    }

}
