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

object AccumuloRDDWriter {

  def write[K: AvroRecordCodec, V: AvroRecordCodec](
    raster: RDD[(K, V)],
    instance: AccumuloInstance,
    encodeKey: K => Key,
    writeStrategy: AccumuloWriteStrategy,
    table: String,
    oneToOne: Boolean = false
  ): Unit = {
    implicit val sc = raster.sparkContext

    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    instance.ensureTableExists(table)

    val kvPairs: RDD[(Key, Value)] = {
      if (oneToOne)
        raster.map { case row => encodeKey(row._1) -> Vector(row) }
      else
        raster.groupBy { row => encodeKey(row._1) }
    }.map { case (key, pairs) =>
      (key, new Value(AvroEncoder.toBinary(pairs.toVector)(codec)))
    }

    writeStrategy.write(kvPairs, instance, table)


  }
}
