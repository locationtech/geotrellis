package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._
import geotrellis.spark.utils.KryoWrapper

import org.apache.accumulo.core.data.{Key, Value}
import org.apache.hadoop.io.Text
import org.apache.spark.rdd.RDD
import org.joda.time.DateTimeZone
import spray.json._

import scala.reflect._

/**
 * This class implements writing SpaceTime keyed RDDs by using Accumulo column qualifier to store the dates of each tile.
 * This allows the use of server side filters for refinement of spatio-temporal queries.
 */
class SpaceTimeAccumuloLayerWriter[V: AvroRecordCodec: ClassTag, M: JsonFormat](
  val attributeStore: AttributeStore[JsonFormat],
  val instance: AccumuloInstance,
  keyIndexMethod: KeyIndexMethod[SpaceTimeKey],
  table: String,
  strategy: AccumuloWriteStrategy = AccumuloWriteStrategy.DEFAULT
) extends Writer[LayerId, RDD[(SpaceTimeKey, V)] with Metadata[M]] {
  val codec  = KeyValueRecordCodec[SpaceTimeKey, V]
  val schema = codec.schema

  def write(id: LayerId, rdd: RDD[(SpaceTimeKey, V)] with Metadata[M]): Unit = {
    val header =
      AccumuloLayerHeader(
        keyClass = classTag[SpaceTimeKey].toString(),
        valueClass = classTag[V].toString(),
        tileTable = table
      )
    val metaData = rdd.metadata
    val keyBounds = implicitly[Boundable[SpaceTimeKey]].getKeyBounds(rdd)
    val keyIndex = keyIndexMethod.createIndex(keyBounds)
    val getRowId = (key: SpaceTimeKey) => index2RowId(keyIndex.toIndex(key))
    val cf =  columnFamily(id)

    try {
      attributeStore.writeLayerAttributes(id, header, metaData, keyBounds, keyIndex, schema)
      instance.ensureTableExists(table)
      instance.makeLocalityGroup(table, cf)

      val timeText = (key: SpaceTimeKey) => new Text(key.time.withZone(DateTimeZone.UTC).toString)

      val _codec = codec
      val kvPairs =
        rdd
          .map { case tuple @ (key, _) =>
            val value: Value = new Value(AvroEncoder.toBinary(Vector(tuple))(_codec))
            val rowKey = new Key(getRowId(key), cf, timeText(key))
            (rowKey, value)
          }

      strategy.write(kvPairs, instance, table)

    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}
