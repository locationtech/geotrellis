package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io.CollectionLayerReader
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.util.Filesystem
import org.apache.avro.Schema

import scalaz.std.vector._
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.{Process, nondeterminism}
import java.io.File
import java.util.concurrent.Executors

object FileCollectionReader {
  def read[K: AvroRecordCodec : Boundable, V: AvroRecordCodec](
    keyPath: Long => String,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    numPartitions: Option[Int] = None): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(CollectionLayerReader.defaultNumPartitions)).toVector.map(_.toIterator)

    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val _recordCodec = KeyValueRecordCodec[K, V]

    val pool = Executors.newFixedThreadPool(32)

    val result = bins flatMap { partition =>
      val range: Process[Task, Iterator[Long]] = Process.unfold(partition) { iter =>
        if (iter.hasNext) {
          val (start, end) = iter.next()
          Some((start to end).toIterator, iter)
        }
        else None
      }

      val read: Iterator[Long] => Process[Task, Vector[(K, V)]] = { iterator =>
          Process.unfold(iterator) { iter =>
            if (iter.hasNext) {
              val index = iter.next()
              val path = keyPath(index)
              if (new File(path).exists) {
                val bytes: Array[Byte] = Filesystem.slurp(path)
                val recs = AvroEncoder.fromBinary(writerSchema.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
                if (filterIndexOnly) Some(recs, iter)
                else Some(recs.filter { row => includeKey(row._1) }, iter)
              } else Some(Vector(), iter)
            } else None
          }
      }

      nondeterminism.njoin(maxOpen = 32, maxQueued = 32) { range map read }(Strategy.Executor(pool)).runFoldMap(identity).unsafePerformSync
    }

    pool.shutdown(); result
  }
}
