package geotrellis.spark.io.index.zcurve

import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex

import com.github.nscala_time.time.Imports._

object ZSpaceTimeKeyIndex {
  def byYear(): ZSpaceTimeKeyIndex = 
    new ZSpaceTimeKeyIndex("Y")

  def byMonth(): ZSpaceTimeKeyIndex =
    new ZSpaceTimeKeyIndex("YMM")

  def byDay(): ZSpaceTimeKeyIndex =
    new ZSpaceTimeKeyIndex("YDDD")

  def byPattern(pattern: String): ZSpaceTimeKeyIndex =
    new ZSpaceTimeKeyIndex(pattern)
}

class ZSpaceTimeKeyIndex(val pattern: String) extends KeyIndex[SpaceTimeKey] {
  private def toZ(key: SpaceTimeKey): Z3 = Z3(key.col, key.row, DateTimeFormat.forPattern(pattern).print(key.time).toInt)

  def toIndex(key: SpaceTimeKey): Long = toZ(key).z

  def indexRanges(keyRange: (SpaceTimeKey, SpaceTimeKey)): Seq[(Long, Long)] =
    Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
}
