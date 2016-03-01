package geotrellis.spark.io.index.zcurve

import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.index.zcurve._

import com.github.nscala_time.time.Imports._

object ZSpaceTimeKeyIndex {
  def byMilliseconds(millis: Long): ZSpaceTimeKeyIndex =
    new ZSpaceTimeKeyIndex({ key =>
      (key.instant / millis).toInt
    })

  def bySecond(): ZSpaceTimeKeyIndex =
    byMilliseconds(1000L)

  def bySeconds(seconds: Int): ZSpaceTimeKeyIndex =
    byMilliseconds(1000L * seconds)

  def byMinute(): ZSpaceTimeKeyIndex =
    byMilliseconds(1000L * 60)

  def byMinutes(minutes: Int): ZSpaceTimeKeyIndex =
    byMilliseconds(1000L * 60 * minutes)

  def byHour(): ZSpaceTimeKeyIndex =
    byMilliseconds(1000L * 60 * 60)

  def byHours(hours: Int): ZSpaceTimeKeyIndex =
    byMilliseconds(1000L * 60 * 60 * hours)

  def byDay(): ZSpaceTimeKeyIndex =
    byMilliseconds(1000L * 60 * 60 * 24)

  def byDays(days: Int): ZSpaceTimeKeyIndex =
    byMilliseconds(1000L * 60 * 60 * 24 * days)

  def byMonth(): ZSpaceTimeKeyIndex =
    byMilliseconds(1000L * 60 * 60 * 30)

  def byMonths(months: Int): ZSpaceTimeKeyIndex =
    byMilliseconds(1000L * 60 * 60 * 30 * months)

  def byYear(): ZSpaceTimeKeyIndex =
    byMilliseconds(1000L * 60 * 60 * 365)

  def byYears(years: Int): ZSpaceTimeKeyIndex =
    byMilliseconds(1000L * 60 * 60 * 365 * years)
}

class ZSpaceTimeKeyIndex(toGrid: SpaceTimeKey => Int) extends KeyIndex[SpaceTimeKey] {
  def keyBounds = None

  private def toZ(key: SpaceTimeKey): Z3 = Z3(key.col, key.row, toGrid(key))

  def toIndex(key: SpaceTimeKey): Long = toZ(key).z

  def indexRanges(keyRange: (SpaceTimeKey, SpaceTimeKey)): Seq[(Long, Long)] =
    Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
}
