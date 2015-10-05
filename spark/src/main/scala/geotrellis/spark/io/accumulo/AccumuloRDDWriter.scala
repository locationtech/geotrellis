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
    val _codec = codec

    val kvPairs: RDD[(Key, Value)] = {
      if (oneToOne)
        raster.map { case row => encodeKey(row._1) -> Vector(row) }
      else
        raster.groupBy { row => encodeKey(row._1) }
    }.map { case (key, pairs) =>
      (key, new Value(AvroEncoder.toBinary(pairs.toVector)(_codec)))
    }

    strategy.write(kvPairs, instance, table)
  }
}

object AccumuloRDDWriter {

}
