package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._

import org.apache.hadoop.io.Text

import org.apache.spark.rdd.RDD

import org.apache.accumulo.core.data.{Key, Value}

import scala.collection.JavaConversions._


class RDDWriter[K: AvroRecordCodec, TileType: AvroRecordCodec](instance: AccumuloInstance){
  def write(
      raster: RDD[(K, TileType)],
      table: String,
      localityGroup: Option[String],
      keyBounds: KeyBounds[K],
      keyIndex: KeyIndex[K],
      getRowId: Long => String,
      getColFamily: K => String,
      getColQualifier: K => String,
      oneToOne: Boolean = false,
      strategy: AccumuloWriteStrategy): Unit = {
    implicit val sc = raster.sparkContext

    // Create table if it doesn't exist.
    val ops = instance.connector.tableOperations()
    if (! ops.exists(table))
      ops.create(table)

    if (localityGroup.isDefined) {
      val groups = ops.getLocalityGroups(table)
      val newGroup: java.util.Set[Text] = Set(new Text(localityGroup.get))
      ops.setLocalityGroups(table, groups.updated(table, newGroup))
    }
    
    val codec  = KeyValueRecordCodec[K, TileType]

    val encodeKey = (key: K) => {
      new Key(getRowId(keyIndex.toIndex(key)), getColFamily(key), getColQualifier(key))
    }

    val kvPairs: RDD[(Key, Value)] = {
      if (oneToOne)
        raster.map { case row => encodeKey(row._1) -> Vector(row) }
      else
        raster.groupBy { row => encodeKey(row._1) }
    }.map { case (key: Key, pairs) =>
      (key, new Value(AvroEncoder.toBinary(pairs.toVector)(codec)))
    }

    strategy.write(kvPairs, instance, table)
  }

}

object RDDWriter {
  /**
   * Mapping KeyBounds of Extent to SFC ranges will often result in a set of non-contigrious ranges.
   * The indices exluded by these ranges should not be included in split calcluation as they will never be seen.
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
