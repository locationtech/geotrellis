package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io.CollectionLayerReader
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.util.Filesystem

import org.apache.avro.Schema
import scalaz.concurrent.Task
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

    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(CollectionLayerReader.defaultNumPartitions)).toVector

    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val _recordCodec = KeyValueRecordCodec[K, V]

    val pool = Executors.newFixedThreadPool(32)

    val result = bins flatMap { partition =>
      val ranges = Process.unfold(partition.toIterator) { iter: Iterator[(Long, Long)] =>
        if (iter.hasNext) Some(iter.next(), iter)
        else None
      }

      val read: ((Long, Long)) => Process[Task, List[(K, V)]] = { case (start, end) =>
        Process eval {
          Task.gatherUnordered(for {
            index <- start to end
          } yield Task {
            val path = keyPath(index)
            if (new File(path).exists) {
              val bytes: Array[Byte] = Filesystem.slurp(path)
              val recs = AvroEncoder.fromBinary(writerSchema.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
              if (filterIndexOnly)
                recs
              else
                recs.filter { row => includeKey(row._1) }
            } else Vector()
          }(pool)).map(_.flatten)
        }
      }

      nondeterminism.njoin(maxOpen = 32, maxQueued = 32) { ranges map read }.runLog.map(_.flatten).unsafePerformSync
    }

    pool.shutdown()
    result
  }
}
