package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.util.cache.LRUCache

import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io._
import org.apache.hadoop.fs.Path

/**
  * incorrect impl
  */
object HadoopCollectionReader {
  def read[
    K: AvroRecordCodec: Boundable,
    V: AvroRecordCodec
  ](path: Path,
    conf: Configuration,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    indexFilterOnly: Boolean,
    maxOpenFiles: Int = 16,
    writerSchema: Option[Schema] = None): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val reader = new MapFile.Reader(path, conf)

    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)
    val indexRanges = queryKeyBounds.flatMap(decomposeBounds).toArray

    val codec = KeyValueRecordCodec[K, V]

    (for {
      range <- indexRanges
      index <- range._1 to range._2
    } yield {
      val keyWritable = new LongWritable(index)
      keyWritable -> reader
        .get(keyWritable, new BytesWritable())
        .asInstanceOf[BytesWritable]
    }).flatMap { case (keyWritable, valueWritable) =>
      val items = AvroEncoder.fromBinary(writerSchema.getOrElse(codec.schema), valueWritable.getBytes)(codec)
      if(indexFilterOnly)
        items
      else
        items.filter { row => includeKey(row._1) }
    }
  }
}
