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

package geotrellis.layers.index

import geotrellis.tiling._
import geotrellis.layers._
import geotrellis.layers.index.hilbert._

import java.time.ZonedDateTime

private[index] trait HilbertKeyIndexMethod

object HilbertKeyIndexMethod extends HilbertKeyIndexMethod {
  implicit def spatialKeyIndexIndex(m: HilbertKeyIndexMethod): KeyIndexMethod[SpatialKey] =
    new KeyIndexMethod[SpatialKey] {
      def createIndex(keyBounds: KeyBounds[SpatialKey]) = {
        val xResolution = resolution(keyBounds.maxKey.col, keyBounds.minKey.col)
        val yResolution = resolution(keyBounds.maxKey.row, keyBounds.minKey.row)
        HilbertSpatialKeyIndex(keyBounds, xResolution, yResolution)
      }
    }

  def apply(temporalResolution: Int): KeyIndexMethod[SpaceTimeKey] =
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = {
        val xResolution = resolution(keyBounds.maxKey.col, keyBounds.minKey.col)
        val yResolution = resolution(keyBounds.maxKey.row, keyBounds.minKey.row)
        HilbertSpaceTimeKeyIndex(keyBounds, xResolution, yResolution, temporalResolution)
      }
    }

  def apply(minDate: ZonedDateTime, maxDate: ZonedDateTime, temporalResolution: Int): KeyIndexMethod[SpaceTimeKey] =
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]): KeyIndex[SpaceTimeKey] = {
        val adjustedKeyBounds = {
          val minKey = keyBounds.minKey
          val maxKey = keyBounds.maxKey
          KeyBounds[SpaceTimeKey](SpaceTimeKey(minKey.col, minKey.row, minDate), SpaceTimeKey(maxKey.col, maxKey.row, maxDate))
        }
        val xResolution = resolution(keyBounds.maxKey.col, keyBounds.minKey.col)
        val yResolution = resolution(keyBounds.maxKey.row, keyBounds.minKey.row)
        HilbertSpaceTimeKeyIndex(adjustedKeyBounds, xResolution, yResolution, temporalResolution)
      }
    }
}
