package geotrellis.layers.file

import java.io.File

import geotrellis.tiling.{Boundable, KeyBounds, EmptyBounds}
import geotrellis.layers.avro.codecs.KeyValueRecordCodec
import geotrellis.layers.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.layers.file.conf.FileConfig
import geotrellis.layers.index.MergeQueue
import geotrellis.layers.util.IOUtils
import geotrellis.util.Filesystem

import org.apache.avro.Schema


object FileCollectionReader {
  val defaultThreadCount: Int = FileConfig.threads.collection.readThreads

  def read[K: AvroRecordCodec : Boundable, V: AvroRecordCodec](
    keyPath: BigInt => String,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    threads: Int = defaultThreadCount): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val _recordCodec = KeyValueRecordCodec[K, V]

    IOUtils.parJoin[K, V](ranges.toIterator, threads) { index: BigInt =>
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
