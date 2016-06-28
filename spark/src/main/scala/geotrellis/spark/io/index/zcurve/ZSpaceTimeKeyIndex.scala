package geotrellis.spark.io.index.zcurve

import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.index.zcurve._

import com.github.nscala_time.time.Imports._

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

  def toIndex(key: SpaceTimeKey): Long = toZ(key).z

  def indexRanges(keyRange: (SpaceTimeKey, SpaceTimeKey)): Seq[(Long, Long)] =
    Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
}
