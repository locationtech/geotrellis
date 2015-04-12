package geotrellis.spark.io.index

import geotrellis.spark._
import geotrellis.index.zcurve._

import com.github.nscala_time.time.Imports._

class YearZSpaceTimeKeyIndex() extends KeyIndex[SpaceTimeKey] {
  private def toZ(key: SpaceTimeKey): Z3 = Z3(key.spatialKey.col, key.spatialKey.row, key.temporalKey.time.getYear)

  def toIndex(key: SpaceTimeKey): Long = toZ(key).z

  def indexRanges(keyRange: (SpaceTimeKey, SpaceTimeKey)): Seq[(Long, Long)] =
    Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
}
