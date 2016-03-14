package geotrellis.spark.partition

import geotrellis.spark._
import geotrellis.spark.io.index.zcurve._

object TestImplicits {
  implicit object TestPartitioner extends PartitionerIndex[SpatialKey] {
    private val zCurveIndex = new ZSpatialKeyIndex(KeyBounds(SpatialKey(0, 0), SpatialKey(100, 100)))

    def rescale(key: SpatialKey): SpatialKey =
      SpatialKey(key.col/2, key.row/2)

    override def toIndex(key: SpatialKey): Long =
      zCurveIndex.toIndex(rescale(key))

    override def indexRanges(r: (SpatialKey, SpatialKey)): Seq[(Long, Long)] =
      zCurveIndex.indexRanges((rescale(r._1), rescale(r._2)))
  }
}
