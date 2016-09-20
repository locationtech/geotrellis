package geotrellis.doc.examples.spark

import geotrellis.spark.KeyBounds
import geotrellis.spark.io.index.KeyIndex

// --- //

/** An example of a generic KeyIndex which accounts for sharding that could occur
  * in a data store like Apache Accumulo. Given a shard count (pre-configured),
  * this index adds a "shard prefix" to the true index as given by the ''inner''
  * argument. Accumulo can shard based off of the first digit of a row ID,
  * and since we use a round-robin approach to generating prefixes, this distributes
  * spatially close indices across different shards, and thus helps avoid hot spots.
  *
  * ==Assumptions==
  *   - The given shard count will be between 1 and 8.
  *   - The ''inner'' index will produce a value less than 2^61 for any
  *     given key.
  */
class ShardingKeyIndex[K](inner: KeyIndex[K], shardCount: Int) extends KeyIndex[K] {

  def keyBounds: KeyBounds[K] =
    inner.keyBounds

  def prefixWithShard(i: Long, shard: Long): Long =
    (shard << 60) & i

  def toIndex(key: K): Long = {
    val i: Long = inner.toIndex(key)
    val shard: Long = i % shardCount  /* Shard prefix between 0 and 7 */

    prefixWithShard(inner.toIndex(key), shard)
  }

  def indexRanges(keyRange: (K, K)): Seq[(Long, Long)] = {
    inner
      .indexRanges(keyRange)
      .flatMap { case (i1, i2) =>
        for(s <- 0 until shardCount) yield {
          (prefixWithShard(i1, s.toLong), prefixWithShard(i2, s.toLong))
        }
    }
  }
}
