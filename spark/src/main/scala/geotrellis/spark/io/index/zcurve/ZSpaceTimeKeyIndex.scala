package geotrellis.spark.io.index.zcurve

import geotrellis.spark._
import geotrellis.spark.io.index._
import com.github.nscala_time.time.Imports._

object ZSpaceTimeKeyIndex {
  def apply(timeToGrid: DateTime => Int): KeyIndex[SpaceTimeKey] =
    new ZSpaceTimeKeyIndex(timeToGrid)

  def byYear(): ZSpaceTimeKeyIndex = 
    new ZSpaceTimeKeyIndex({ dt => dt.getYear })
}

class ZSpaceTimeKeyIndex(timeToGrid: DateTime => Int) extends KeyIndex[SpaceTimeKey] {
  private def toZ(key: SpaceTimeKey): Z3 = Z3(key.col, key.row, timeToGrid(key.time))

  def toIndex(key: SpaceTimeKey): Long = toZ(key).z

  def indexRanges(keyRange: (SpaceTimeKey, SpaceTimeKey)): Seq[(Long, Long)] =
    Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
}
