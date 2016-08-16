package geotrellis.spark.io.accumulo

import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.{Boundable, KeyBounds}

import org.apache.accumulo.core.data.{Range => AccumuloRange}
import org.apache.accumulo.core.security.Authorizations
import org.apache.avro.Schema
import org.apache.hadoop.io.Text

import scala.collection.JavaConversions._
import scala.reflect.ClassTag

object AccumuloCollectionReader {
  def read[K: Boundable: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](
    table: String,
    columnFamily: Text,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[AccumuloRange],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None
  )(implicit instance: AccumuloInstance): Seq[(K, V)] = {
    if(queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val codec = KeyValueRecordCodec[K, V]
    val includeKey = (key: K) => queryKeyBounds.includeKey(key)

    val ranges = queryKeyBounds.flatMap(decomposeBounds)

    ranges flatMap { range: AccumuloRange =>
      val scanner = instance.connector.createScanner(table, new Authorizations())
      scanner.setRange(range)
      scanner.fetchColumnFamily(columnFamily)
      scanner.iterator.map { case entry =>
        AvroEncoder.fromBinary(writerSchema.getOrElse(codec.schema), entry.getValue.get)(codec)
      }.flatMap { pairs: Vector[(K, V)] =>
        if(filterIndexOnly) pairs
        else pairs.filter { pair => includeKey(pair._1) }
      }
    }
  }
}
