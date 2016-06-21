package geotrellis.spark.io.index

import geotrellis.spark._
import geotrellis.spark.io.index.zcurve._

import com.github.nscala_time.time.Imports._

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
