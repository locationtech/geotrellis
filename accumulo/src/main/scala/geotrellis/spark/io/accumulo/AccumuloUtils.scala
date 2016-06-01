package geotrellis.spark.io.accumulo

import geotrellis.spark.{ Bounds, Boundable, KeyBounds, EmptyBounds }
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}

import org.apache.accumulo.core.data.Key
import org.apache.hadoop.io.Text
import org.apache.spark.rdd.RDD

import scala.collection.JavaConverters._


object AccumuloUtils {
  /**
   * Mapping KeyBounds of Extent to SFC ranges will often result in a set of non-contigrious ranges.
   * The indices exluded by these ranges should not be included in split calculation as they will never be seen.
   */
  def getSplits[K](kb: KeyBounds[K], ki: KeyIndex[K], count: Int): Seq[Long] = {
    var stack = ki.indexRanges(kb).toList
    def len(r: (Long, Long)) = r._2 - r._1 + 1l
    val total = stack.foldLeft(0l){ (s,r) => s + len(r) }
    val binWidth = total / count

    def splitRange(range: (Long, Long), take: Long): ((Long, Long), (Long, Long)) = {
      assert(len(range) > take)
      assert(take > 0)
      (range._1, range._1 + take - 1) -> (range._1 + take, range._2)
    }

    val arr = Array.fill[Long](count - 1)(0)
    var sum = 0l
    var i = 0

    while (i < count - 1) {
      val nextStep = sum + len(stack.head)
      if (nextStep < binWidth){
        sum += len(stack.head)
        stack = stack.tail
      } else if (nextStep == binWidth) {
        arr(i) = stack.head._2
        stack = stack.tail
        i += 1
        sum = 0l
      } else {
        val (take, left) = splitRange(stack.head, binWidth - sum)
        stack = left :: stack.tail
        arr(i) = take._2
        i += 1
        sum = 0l
      }
    }
    arr
  }

  /**
    * Split the given Accumulo table into the given number of tablets.
    * This should improve the ingest performance, as it will allow
    * more than one tablet server to participate in the ingestion.
    *
    * @param  tableName         The name of the table to be split
    * @param  accumuloInstnace  The Accumulo instance associated with the ingest
    * @param  keyBounds         The KeyBounds of the RDD that is being stored in the table
    * @param  keyIndexer        The indexing scheme used to turn keys K into Accumulo keys
    * @param  count             The number of tablets to split the table into
    */
  def addSplits[K](
    tableName: String,
    accumuloInstance: AccumuloInstance,
    keyBounds: KeyBounds[K],
    keyIndexer: KeyIndex[K],
    count: Int
  ) = {
    val ops = accumuloInstance.connector.tableOperations

    val splits = AccumuloUtils
      .getSplits(keyBounds, keyIndexer, count)
      .map({ i => AccumuloKeyEncoder.index2RowId(i) })

    ops.addSplits(tableName, new java.util.TreeSet(splits.asJava))
  }

}
