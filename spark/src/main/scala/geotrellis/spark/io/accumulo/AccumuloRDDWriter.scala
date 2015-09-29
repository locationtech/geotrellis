package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.utils.KryoWrapper
import org.apache.avro.Schema

import org.apache.hadoop.io.Text

import org.apache.spark.rdd.RDD

import org.apache.accumulo.core.data.{Key, Value}

import scala.collection.JavaConversions._

trait BaseAccumuloRDDWriter[K, TileType] {
  def schema: Schema
  def instance: AccumuloInstance

  def ensureTableExists(tableName: String): Unit = {
    val ops = instance.connector.tableOperations()
    if (! ops.exists(tableName))
      ops.create(tableName)
  }

  def makeLocalityGroup(tableName: String, columnFamily: String): Unit = {
    val ops = instance.connector.tableOperations()
    val groups = ops.getLocalityGroups(tableName)
    val newGroup: java.util.Set[Text] = Set(new Text(columnFamily))
    ops.setLocalityGroups(tableName, groups.updated(tableName, newGroup))
  }
  
  def write(raster: RDD[(K, TileType)], table: String, columnFamily: String, keyToRowId: (K) => Text, oneToOne: Boolean = false): Unit
}

class AccumuloRDDWriter[K: AvroRecordCodec, V: AvroRecordCodec](
    val instance: AccumuloInstance,
    strategy: AccumuloWriteStrategy)
  extends BaseAccumuloRDDWriter[K, V] {

  val codec  = KeyValueRecordCodec[K, V]
  val schema = codec.schema

  def write(
      raster: RDD[(K, V)],
      table: String,
      columnFamily: String,
      keyToRowId: (K) => Text,
      oneToOne: Boolean = false): Unit = {
    implicit val sc = raster.sparkContext

    ensureTableExists(table)
    makeLocalityGroup(table, columnFamily.toString)

    val encodeKey = (key: K) => new Key(keyToRowId(key), columnFamily)

    val kwCodec = KryoWrapper(codec)
    val kvPairs: RDD[(Key, Value)] = {
      if (oneToOne)
        raster.map { case row => encodeKey(row._1) -> Vector(row) }
      else
        raster.groupBy { row => encodeKey(row._1) }
    }.map { case (key, pairs) =>
      (key, new Value(AvroEncoder.toBinary(pairs.toVector)(kwCodec.value)))
    }

    strategy.write(kvPairs, instance, table)
  }
}

object AccumuloRDDWriter {
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
