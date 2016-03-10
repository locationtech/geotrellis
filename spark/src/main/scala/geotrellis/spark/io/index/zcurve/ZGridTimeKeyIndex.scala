package geotrellis.spark.io.index.zcurve

import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.index.zcurve._

import com.github.nscala_time.time.Imports._

object ZGridTimeKeyIndex {
  def byMilliseconds(keyBounds: KeyBounds[GridTimeKey], millis: Long): ZGridTimeKeyIndex =
    new ZGridTimeKeyIndex(keyBounds, millis)

  def bySecond(keyBounds: KeyBounds[GridTimeKey]): ZGridTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L)

  def bySeconds(keyBounds: KeyBounds[GridTimeKey], seconds: Int): ZGridTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * seconds)

  def byMinute(keyBounds: KeyBounds[GridTimeKey]): ZGridTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60)

  def byMinutes(keyBounds: KeyBounds[GridTimeKey], minutes: Int): ZGridTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * minutes)

  def byHour(keyBounds: KeyBounds[GridTimeKey]): ZGridTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60)

  def byHours(keyBounds: KeyBounds[GridTimeKey], hours: Int): ZGridTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * hours)

  def byDay(keyBounds: KeyBounds[GridTimeKey]): ZGridTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 24)

  def byDays(keyBounds: KeyBounds[GridTimeKey], days: Int): ZGridTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 24 * days)

  def byMonth(keyBounds: KeyBounds[GridTimeKey]): ZGridTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 30)

  def byMonths(keyBounds: KeyBounds[GridTimeKey], months: Int): ZGridTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 30 * months)

  def byYear(keyBounds: KeyBounds[GridTimeKey]): ZGridTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 365)

  def byYears(keyBounds: KeyBounds[GridTimeKey], years: Int): ZGridTimeKeyIndex =
    byMilliseconds(keyBounds, 1000L * 60 * 60 * 365 * years)
}

class ZGridTimeKeyIndex(val keyBounds: KeyBounds[GridTimeKey], val temporalResolution: Long) extends KeyIndex[GridTimeKey] {
  private def toZ(key: GridTimeKey): Z3 = Z3(key.col, key.row, (key.instant / temporalResolution).toInt)

  def toIndex(key: GridTimeKey): Long = toZ(key).z

  def indexRanges(keyRange: (GridTimeKey, GridTimeKey)): Seq[(Long, Long)] =
    Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
}
