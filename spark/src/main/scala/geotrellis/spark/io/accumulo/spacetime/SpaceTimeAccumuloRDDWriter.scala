package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark.io.accumulo._
import geotrellis.spark.SpaceTimeKey
import geotrellis.spark.io.accumulo.{AccumuloInstance, BaseAccumuloRDDWriter}
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import org.apache.accumulo.core.data.{Key, Value}
import org.apache.hadoop.io.Text
import org.apache.spark.rdd.RDD
import org.joda.time.DateTimeZone

import scala.collection.JavaConversions._

/**
 * This class implements writing SpaceTime keyed RDDs by using Accumulo column qualifier to store the dates of each tile.
 * This allows the use of server side filters for refinement of spatio-temporal queries.
 */
class SpaceTimeAccumuloRDDWriter[TileType: AvroRecordCodec](
    val instance: AccumuloInstance,
    strategy: AccumuloWriteStrategy
  ) extends BaseAccumuloRDDWriter[SpaceTimeKey, TileType] {
  type K = SpaceTimeKey

  val codec  = KeyValueRecordCodec[K, TileType]
  val schema = codec.schema

  def write(raster: RDD[(K, TileType)], table: String, columnFamily: String, keyToRowId: (K) => Text, oneToOne: Boolean = false): Unit = {
    implicit val sc = raster.sparkContext

    val ops = instance.connector.tableOperations()
    if (! ops.exists(table))
      ops.create(table)

    val groups = ops.getLocalityGroups(table)
    val newGroup: java.util.Set[Text] = Set(new Text(columnFamily))
    ops.setLocalityGroups(table, groups.updated(table, newGroup))

    val timeText = (key: K) =>
      new Text(key.temporalKey.time.withZone(DateTimeZone.UTC).toString)

    val kvPairs = raster
      .map { case tuple @ (key, _) =>
        val value = new Value(AvroEncoder.toBinary(Vector(tuple))(codec))
        val rowKey = new Key(keyToRowId(key), columnFamily, timeText(key))
        (rowKey, value)
      }

    strategy.write(kvPairs, instance, table)
  }
}

