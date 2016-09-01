package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.MergeQueue
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.util.Filesystem

import org.apache.avro.Schema
import com.typesafe.config.ConfigFactory

import java.io.File

object FileCollectionReader {
  def read[K: AvroRecordCodec : Boundable, V: AvroRecordCodec](
    keyPath: Long => String,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    threads: Int = ConfigFactory.load().getThreads("geotrellis.file.threads.collection.read")): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val _recordCodec = KeyValueRecordCodec[K, V]

    LayerReader.njoin[K, V](ranges.toIterator, threads) { index: Long =>
      val path = keyPath(index)
      if (new File(path).exists) {
        val bytes: Array[Byte] = Filesystem.slurp(path)
        val recs = AvroEncoder.fromBinary(writerSchema.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
        if (filterIndexOnly) recs
        else recs.filter { row => includeKey(row._1) }
      } else Vector.empty
    }
  }
}
