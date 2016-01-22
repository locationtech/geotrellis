package geotrellis.spark.io.index.zcurve

import geotrellis.spark._
import geotrellis.spark.io.index.{BoundedKeyIndex, KeyIndex}

object ZSpaceTimeKeyIndex {
  val MILLIS_PER_SECOND = 1000l
  val MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60
  val MILLIS_PER_HOUR   = MILLIS_PER_MINUTE * 60
  val MILLIS_PER_DAY    = MILLIS_PER_HOUR * 24
  val MILLIS_PER_MONTH  = MILLIS_PER_DAY * 30
  val MILLIS_PER_YEAR   = MILLIS_PER_DAY * 365

  def byYear(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeKeyIndex = byMillisecondResolution(keyBounds, MILLIS_PER_YEAR)

  def byMonth(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeKeyIndex = byMillisecondResolution(keyBounds, MILLIS_PER_MONTH)

  def byDay(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeKeyIndex = byMillisecondResolution(keyBounds, MILLIS_PER_DAY)

  def byHour(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeKeyIndex = byMillisecondResolution(keyBounds, MILLIS_PER_HOUR)

  def byMinute(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeKeyIndex = byMillisecondResolution(keyBounds, MILLIS_PER_MINUTE)

  def bySecond(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeKeyIndex = byMillisecondResolution(keyBounds, MILLIS_PER_SECOND)

  def byMillisecond(keyBounds: KeyBounds[SpaceTimeKey]): ZSpaceTimeKeyIndex = byMillisecondResolution(keyBounds)

  def byMillisecondResolution(keyBounds: KeyBounds[SpaceTimeKey], millis: Long = 1): ZSpaceTimeKeyIndex =
    new ZSpaceTimeKeyIndex(keyBounds, millis)
}

class ZSpaceTimeKeyIndex(val keyBounds: KeyBounds[SpaceTimeKey], val temporalResolution: Long) extends BoundedKeyIndex[SpaceTimeKey] {
  private def toZ(key: SpaceTimeKey): Z3 = Z3(key.col, key.row, (key.instant / temporalResolution).toInt)

  def toIndex(key: SpaceTimeKey): Long = toZ(key).z

  def indexRanges(keyRange: (SpaceTimeKey, SpaceTimeKey)): Seq[(Long, Long)] =
    Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
}
