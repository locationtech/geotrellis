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

package geotrellis.layers.index.zcurve

import geotrellis.tiling.{SpaceTimeKey, KeyBounds}
import geotrellis.layers._
import geotrellis.layers.index.KeyIndex
import geotrellis.layers.index.zcurve._

object ZSpaceTimeKeyIndex {
  def byMilliseconds(keyBounds: KeyBounds[SpaceTimeKey], millis: Long): ZSpaceTimeKeyIndex =
    new ZSpaceTimeKeyIndex(keyBounds, millis)

  def bySecond(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L)

  def bySeconds(keyBounds: KeyBounds[SpaceTimeKey], seconds: Int): ZSpaceTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * seconds)

  def byMinute(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60)

  def byMinutes(keyBounds: KeyBounds[SpaceTimeKey], minutes: Int): ZSpaceTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * minutes)

  def byHour(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60)

  def byHours(keyBounds: KeyBounds[SpaceTimeKey], hours: Int): ZSpaceTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * hours)

  def byDay(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 24)

  def byDays(keyBounds: KeyBounds[SpaceTimeKey], days: Int): ZSpaceTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 24 * days)

  def byMonth(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 24 * 30)

  def byMonths(keyBounds: KeyBounds[SpaceTimeKey], months: Int): ZSpaceTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 24 * 30 * months)

  def byYear(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 24 * 365)

  def byYears(keyBounds: KeyBounds[SpaceTimeKey], years: Int): ZSpaceTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 24 * 365 * years)
}

class ZSpaceTimeKeyIndex(val keyBounds: KeyBounds[SpaceTimeKey], val temporalResolution: Long) extends KeyIndex[SpaceTimeKey] {
  private def toZ(key: SpaceTimeKey): Z3 = Z3(key.col, key.row, (key.instant / temporalResolution).toInt)

  def toIndex(key: SpaceTimeKey): BigInt = toZ(key).z

  def indexRanges(keyRange: (SpaceTimeKey, SpaceTimeKey)): Seq[(BigInt, BigInt)] =
    Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
}
