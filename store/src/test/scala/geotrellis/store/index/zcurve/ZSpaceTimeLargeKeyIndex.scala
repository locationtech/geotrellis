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

package geotrellis.store.index.zcurve

import geotrellis.layer._
import geotrellis.store.index.KeyIndex

import io.circe._
import io.circe.syntax._

object ZSpaceTimeLargeKeyIndex {
  val NAME = "zorderlarge"

  def byMilliseconds(keyBounds: KeyBounds[SpaceTimeKey], millis: Long, shift: Long = Long.MinValue / 2): ZSpaceTimeLargeKeyIndex =
    new ZSpaceTimeLargeKeyIndex(keyBounds, millis, shift)

  def bySecond(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeLargeKeyIndex =
    byMilliseconds(keyBounds, 1000L)

  def bySeconds(keyBounds: KeyBounds[SpaceTimeKey], seconds: Int): ZSpaceTimeLargeKeyIndex =
    byMilliseconds(keyBounds, 1000L * seconds)

  def byMinute(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeLargeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60)

  def byMinutes(keyBounds: KeyBounds[SpaceTimeKey], minutes: Int): ZSpaceTimeLargeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * minutes)

  def byHour(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeLargeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60)

  def byHours(keyBounds: KeyBounds[SpaceTimeKey], hours: Int): ZSpaceTimeLargeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * hours)

  def byDay(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeLargeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 24)

  def byDays(keyBounds: KeyBounds[SpaceTimeKey], days: Int): ZSpaceTimeLargeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 24 * days)

  def byMonth(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeLargeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 24 * 30)

  def byMonths(keyBounds: KeyBounds[SpaceTimeKey], months: Int): ZSpaceTimeLargeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 24 * 30 * months)

  def byYear(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeLargeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 24 * 365)

  def byYears(keyBounds: KeyBounds[SpaceTimeKey], years: Int): ZSpaceTimeLargeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 24 * 365 * years)

  implicit val zSpaceTimeKeyLargeIndexEncoder: Encoder[ZSpaceTimeLargeKeyIndex] =
    Encoder.encodeJson.contramap[ZSpaceTimeLargeKeyIndex] { obj =>
      Json.obj(
        "type"   -> NAME.asJson,
        "properties" -> Json.obj(
          "keyBounds"   -> obj.keyBounds.asJson,
          "temporalResolution" -> obj.temporalResolution.asJson,
          "shift" -> obj.shift.asJson
        )
      )
    }

  implicit val zSpaceTimeKeyLargeIndexDecoder: Decoder[ZSpaceTimeLargeKeyIndex] =
    Decoder.decodeHCursor.emap { c: HCursor =>
      (c.downField("type").as[String], c.downField("properties")) match {
        case (Right(typeName), properties) =>
          if(typeName != NAME) Left(s"Wrong KeyIndex type: $NAME expected.")
          else {
            (properties.downField("keyBounds").as[KeyBounds[SpaceTimeKey]],
              properties.downField("temporalResolution").as[Long],
              properties.downField("shift").as[Long]) match {
              case (Right(keyBounds), Right(tr), Right(s)) => Right(ZSpaceTimeLargeKeyIndex.byMilliseconds(keyBounds, tr, s))
              case _ => Left("Wrong KeyIndex constructor arguments: ZSpaceTimeLargeKeyIndex constructor arguments expected.")
            }
          }

        case _ => Left("Wrong KeyIndex type: ZSpaceTimeLargeKeyIndex expected.")
      }
    }
}

class ZSpaceTimeLargeKeyIndex(val keyBounds: KeyBounds[SpaceTimeKey], val temporalResolution: Long, val shift: Long = Long.MinValue / 2) extends KeyIndex[SpaceTimeKey] {
  private def toZ(key: SpaceTimeKey): Z3 = Z3(key.col, key.row, ((key.instant - shift) / temporalResolution).toInt)

  def toIndex(key: SpaceTimeKey): BigInt = toZ(key).z

  def indexRanges(keyRange: (SpaceTimeKey, SpaceTimeKey)): Seq[(BigInt, BigInt)] =
    Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
}

