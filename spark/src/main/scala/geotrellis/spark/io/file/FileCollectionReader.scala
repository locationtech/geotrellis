package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.util.Filesystem

import org.apache.avro.Schema
import spire.syntax.cfor._
import scalaz.concurrent._
import scalaz._

import scala.collection.mutable
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

    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(1)).toVector

    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val _recordCodec = KeyValueRecordCodec[K, V]

    val pool = Executors.newFixedThreadPool(64)

    val res = Nondeterminism[Task].gatherUnordered(bins map { partition =>
      val tasks = partition map { case (start, end) => Task.fork {
        Task {
          val resultPartition = new mutable.ArrayBuffer[Vector[(K, V)]]()

          cfor(start)(_ <= end, _ + 1) { index =>
            val path = keyPath(index)
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
        }
      }(pool) }

      Nondeterminism[Task].gatherUnordered(tasks).map(_.flatten)
    }).map(_.flatten).unsafePerformSync

    pool.shutdown()
    res
  }
}
