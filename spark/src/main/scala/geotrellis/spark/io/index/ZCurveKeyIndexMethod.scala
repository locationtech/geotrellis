package geotrellis.spark.io.index

import geotrellis.spark._
import geotrellis.spark.io.index.zcurve._

import com.github.nscala_time.time.Imports._

private[index] trait ZCurveKeyIndexMethod

object ZCurveKeyIndexMethod extends ZCurveKeyIndexMethod {
  implicit def spatialKeyIndexMethod(m: ZCurveKeyIndexMethod): KeyIndexMethod[GridKey] =
    new KeyIndexMethod[GridKey] {
      def createIndex(keyBounds: KeyBounds[GridKey]): KeyIndex[GridKey] =
        new ZGridKeyIndex(keyBounds)
    }

  def byMilliseconds(millis: Long): KeyIndexMethod[GridTimeKey] =
    new KeyIndexMethod[GridTimeKey] {
      def createIndex(keyBounds: KeyBounds[GridTimeKey]) = ZGridTimeKeyIndex.byMilliseconds(keyBounds, millis)
    }

  def bySecond(): KeyIndexMethod[GridTimeKey] =
    byMilliseconds(1000L)

  def bySeconds(seconds: Int): KeyIndexMethod[GridTimeKey] =
    byMilliseconds(1000L * seconds)

  def byMinute(): KeyIndexMethod[GridTimeKey] =
    byMilliseconds(1000L * 60)

  def byMinutes(minutes: Int): KeyIndexMethod[GridTimeKey] =
    byMilliseconds(1000L * 60 * minutes)

  def byHour(): KeyIndexMethod[GridTimeKey] =
    byMilliseconds(1000L * 60 * 60)

  def byHours(hours: Int): KeyIndexMethod[GridTimeKey] =
    byMilliseconds(1000L * 60 * 60 * hours)

  def byDay(): KeyIndexMethod[GridTimeKey] =
    byMilliseconds(1000L * 60 * 60 * 24)

  def byDays(days: Int): KeyIndexMethod[GridTimeKey] =
    byMilliseconds(1000L * 60 * 60 * 24 * days)

  def byMonth(): KeyIndexMethod[GridTimeKey] =
    byMilliseconds(1000L * 60 * 60 * 30)

  def byMonths(months: Int): KeyIndexMethod[GridTimeKey] =
    byMilliseconds(1000L * 60 * 60 * 30 * months)

  def byYear(): KeyIndexMethod[GridTimeKey] =
    byMilliseconds(1000L * 60 * 60 * 365)

  def byYears(years: Int): KeyIndexMethod[GridTimeKey] =
    byMilliseconds(1000L * 60 * 60 * 365 * years)
}
