package geotrellis.spark.io.index.zcurve

import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.index.zcurve._

import com.github.nscala_time.time.Imports._

object ZSpaceTimeKeyIndex {
  def apply(timeToGrid: DateTime => Int): KeyIndex[SpaceTimeKey] =
    new ZSpaceTimeKeyIndex({ key => timeToGrid(key.time) })

  def byYear(): ZSpaceTimeKeyIndex =
    new ZSpaceTimeKeyIndex({ key => key.time.getYear })

  def byPattern(pattern: String): ZSpaceTimeKeyIndex =
    new ZSpaceTimeKeyIndex({ key =>
      DateTimeFormat.forPattern(pattern).print(key.time).toInt
    })

  def byMillisecondResolution(millis: Long): ZSpaceTimeKeyIndex =
    new ZSpaceTimeKeyIndex({ key =>
      (key.instant / millis).toInt
    })
}

class ZSpaceTimeKeyIndex(toGrid: SpaceTimeKey => Int) extends KeyIndex[SpaceTimeKey] {
  private def toZ(key: SpaceTimeKey): Z3 = Z3(key.col, key.row, toGrid(key))

  def toIndex(key: SpaceTimeKey): Long = toZ(key).z

  def indexRanges(keyRange: (SpaceTimeKey, SpaceTimeKey)): Seq[(Long, Long)] =
    Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
}
