package geotrellis.spark.io.accumulo

import geotrellis.spark.{Boundable, KeyBounds}
import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import org.apache.hadoop.io.Text
import org.apache.spark.rdd.RDD

object AccumuloUtils {
  /**
   * Collect keyBounds from the rdd and use given keyIndexMethod to generate n balanced split points for covered space.
   */
  def getSplits[K: Boundable](rdd: RDD[(K, V)] forSome {type V}, keyIndexMethod: KeyIndexMethod[K], n: Int): Seq[Text] = {
    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
    val keyIndex = keyIndexMethod.createIndex(keyBounds)

    getSplits(keyBounds, keyIndex, n).map(index2RowId)
  }

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
}
