package geotrellis.spark.io.index.zcurve

import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.index.zcurve._

class ZGridKeyIndex(val keyBounds: KeyBounds[GridKey]) extends KeyIndex[GridKey] {
  private def toZ(key: GridKey): Z2 = Z2(key.col, key.row)

  def toIndex(key: GridKey): Long = toZ(key).z

  def indexRanges(keyRange: (GridKey, GridKey)): Seq[(Long, Long)] =
    Z2.zranges(toZ(keyRange._1), toZ(keyRange._2))
}
