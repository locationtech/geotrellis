package geotrellis.spark.io.index

trait KeyIndex[K] extends Serializable {
  def toIndex(key: K): Long
  def indexRanges(keyRange: (K, K)): Seq[(Long, Long)]
}
