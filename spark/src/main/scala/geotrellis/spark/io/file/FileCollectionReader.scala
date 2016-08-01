package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.util.Filesystem
import org.apache.avro.Schema
import spire.syntax.cfor._

import scala.collection.mutable
import java.io.File
import java.util.concurrent.Executors


import scala.collection.mutable.ArrayBuffer
import scalaz.concurrent._
import scalaz.stream._
import scalaz._

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

    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(1)).toVector

    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val _recordCodec = KeyValueRecordCodec[K, V]

    bins flatMap { partition =>
      val ranges: Process[Task, (Long, Long)] = Process.unfold(partition.toIterator) { iter =>
        if (iter.hasNext) {
          Some(iter.next(), iter)
        } else {
          None
        }
      }

      val pool = Executors.newFixedThreadPool(8)

      val read: ((Long, Long)) => Process[Task, ArrayBuffer[(K, V)]] = { case (start, end) =>
        Process eval Task {
          val resultPartition = new mutable.ArrayBuffer[Vector[(K, V)]]()

          cfor(start)(_ <= end, _ + 1) { index =>
            val path   = keyPath(index)
            val result = if (new File(path).exists) {
              val bytes: Array[Byte] = Filesystem.slurp(path)
              val recs = AvroEncoder.fromBinary(writerSchema.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
              if (filterIndexOnly)
                recs
              else
                recs.filter { row => includeKey(row._1) }
            } else Vector()

            resultPartition += result
          }

          resultPartition.flatten
        }(pool)
      }

      nondeterminism.njoin(maxOpen = 8, maxQueued = 8) { ranges map read }.runLog.unsafePerformSync.flatten
    }
  }
}
