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
  *   - The ''inner'' index will produce a value less than 2^60 for any
  *     given key.
  */
class ShardingKeyIndex[K](inner: KeyIndex[K], shardCount: Int) extends KeyIndex[K] {

  /* Necessary for extending `KeyIndex` */
  def keyBounds: KeyBounds[K] =
    inner.keyBounds

  /** Prefix the shard bits to the original index. Example:
    *
    * {{{
    * val i: Long = 37  // ... 0010 0101
    * val s: Long = 7   // ... 0000 0111
    *
    * // 0111 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0010 0101
    * prefixWithShard(i, s) == 8070450532247928869
    * }}}
    */
  private def prefixWithShard(i: Long, shard: Long): Long =
    (shard << 60) | i

  /* Necessary for extending `KeyIndex` */
  def toIndex(key: K): Long = {
    val i: Long = inner.toIndex(key)
    val shard: Long = i % shardCount  /* Shard prefix between 0 and 7 */

    prefixWithShard(inner.toIndex(key), shard)
  }

  /* Necessary for extending `KeyIndex` */
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
