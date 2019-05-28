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
import geotrellis.layers.index.zcurve._

private[index] trait ZCurveKeyIndexMethod

object ZCurveKeyIndexMethod extends ZCurveKeyIndexMethod {
  implicit def spatialKeyIndexMethod(m: ZCurveKeyIndexMethod): KeyIndexMethod[SpatialKey] =
    new KeyIndexMethod[SpatialKey] {
      def createIndex(keyBounds: KeyBounds[SpatialKey]): KeyIndex[SpatialKey] =
        new ZSpatialKeyIndex(keyBounds)
    }

  def byMilliseconds(millis: Long): KeyIndexMethod[SpaceTimeKey] =
    new KeyIndexMethod[SpaceTimeKey] {
      def createIndex(keyBounds: KeyBounds[SpaceTimeKey]) = ZSpaceTimeKeyIndex.byMilliseconds(keyBounds, millis)
    }

  def bySecond(): KeyIndexMethod[SpaceTimeKey] =
    byMilliseconds(1000L)

  def bySeconds(seconds: Int): KeyIndexMethod[SpaceTimeKey] =
    byMilliseconds(1000L * seconds)

  def byMinute(): KeyIndexMethod[SpaceTimeKey] =
    byMilliseconds(1000L * 60)

  def byMinutes(minutes: Int): KeyIndexMethod[SpaceTimeKey] =
    byMilliseconds(1000L * 60 * minutes)

  def byHour(): KeyIndexMethod[SpaceTimeKey] =
    byMilliseconds(1000L * 60 * 60)

  def byHours(hours: Int): KeyIndexMethod[SpaceTimeKey] =
    byMilliseconds(1000L * 60 * 60 * hours)

  def byDay(): KeyIndexMethod[SpaceTimeKey] =
    byMilliseconds(1000L * 60 * 60 * 24)

  def byDays(days: Int): KeyIndexMethod[SpaceTimeKey] =
    byMilliseconds(1000L * 60 * 60 * 24 * days)

  def byMonth(): KeyIndexMethod[SpaceTimeKey] =
    byMilliseconds(1000L * 60 * 60 * 24 * 30)

  def byMonths(months: Int): KeyIndexMethod[SpaceTimeKey] =
    byMilliseconds(1000L * 60 * 60 * 24 * 30 * months)

  def byYear(): KeyIndexMethod[SpaceTimeKey] =
    byMilliseconds(1000L * 60 * 60 * 24 * 365)

  def byYears(years: Int): KeyIndexMethod[SpaceTimeKey] =
    byMilliseconds(1000L * 60 * 60 * 24 * 365 * years)
}
