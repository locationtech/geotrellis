package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark.io.accumulo._
import geotrellis.spark.SpaceTimeKey
import geotrellis.spark.io.accumulo.{AccumuloInstance, BaseAccumuloRDDWriter}
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.utils.KryoWrapper
import org.apache.accumulo.core.data.{Key, Value}
import org.apache.hadoop.io.Text
import org.apache.spark.rdd.RDD
import org.joda.time.DateTimeZone

/**
 * This class implements writing SpaceTime keyed RDDs by using Accumulo column qualifier to store the dates of each tile.
 * This allows the use of server side filters for refinement of spatio-temporal queries.
 */
class SpaceTimeAccumuloRDDWriter[V: AvroRecordCodec](
    val instance: AccumuloInstance,
    strategy: AccumuloWriteStrategy
  ) extends BaseAccumuloRDDWriter[SpaceTimeKey, V] {
  type K = SpaceTimeKey

  val codec  = KeyValueRecordCodec[K, V]
  val schema = codec.schema

  def write(raster: RDD[(K, V)], table: String, columnFamily: String, keyToRowId: (K) => Text, oneToOne: Boolean = false): Unit = {
    implicit val sc = raster.sparkContext

    ensureTableExists(table)
    makeLocalityGroup(table, columnFamily)

    val timeText = (key: K) =>
      new Text(key.time.withZone(DateTimeZone.UTC).toString)

    val kwCodec = KryoWrapper(codec)
    val kvPairs = raster
      .map { case tuple @ (key, _) =>
        val value: Value = new Value(AvroEncoder.toBinary(Vector(tuple))(kwCodec.value))
        val rowKey = new Key(keyToRowId(key), columnFamily, timeText(key))
        (rowKey, value)
      }

    strategy.write(kvPairs, instance, table)
  }
}

