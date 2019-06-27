package geotrellis.store.file

import geotrellis.layer._
import geotrellis.store.avro.codecs.KeyValueRecordCodec
import geotrellis.store.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.store.index.MergeQueue
import geotrellis.store.util.IOUtils
import geotrellis.util.Filesystem

import org.apache.avro.Schema
import java.io.File

import scala.concurrent.ExecutionContext

object FileCollectionReader {
  def read[K: AvroRecordCodec : Boundable, V: AvroRecordCodec](
    keyPath: BigInt => String,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(BigInt, BigInt)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None
  )(implicit ec: ExecutionContext): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val _recordCodec = KeyValueRecordCodec[K, V]

    IOUtils.parJoin[K, V](ranges.toIterator) { index: BigInt =>
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
