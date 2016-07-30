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

import scalaz.concurrent.Task
import scalaz.concurrent.Future
import scalaz.stream.{Process, nondeterminism}

object FileCollectionReader {
  def read[K: AvroRecordCodec: Boundable, V: AvroRecordCodec](
    keyPath: Long => String,
    queryKeyBounds: Seq[KeyBounds[K]],
    decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
    filterIndexOnly: Boolean,
    writerSchema: Option[Schema] = None,
    numPartitions: Option[Int] = None): Seq[(K, V)] = {
    if(queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(1))

    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val _recordCodec = KeyValueRecordCodec[K, V]

    bins map { partition =>
      partition map { case (start, end) => Task {
        val resultPartition = new mutable.ArrayBuffer[Task[Vector[(K, V)]]]()

        cfor(start)(_ <= end, _ + 1) { index => resultPartition += Task {
          val path = keyPath(index)
          if (new File(path).exists) {
            val bytes: Array[Byte] = Filesystem.slurp(path)
            val recs = AvroEncoder.fromBinary(writerSchema.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
            if (filterIndexOnly)
              recs
            else
              recs.filter { row => includeKey(row._1) }
          } else Vector()
        } }

        Task.gatherUnordered(resultPartition).unsafePerformSync.flatten

      } }

    }

    val requests: Process[Task, (K, V)] =
      Process.unfold(partition.toIterator){ iter =>
        if (iter.hasNext) {
          val recs = iter.next()
          val key = recs._1
          val pairs = recs._2.toVector
          val bytes = AvroEncoder.toBinary(pairs)(_codec)
          val metadata = new ObjectMetadata()
          metadata.setContentLength(bytes.length)
          val is = new ByteArrayInputStream(bytes)
          val request = putObjectModifier(new PutObjectRequest(bucket, key, is, metadata))
          Some(request, iter)
        } else  {
          None
        }
      }

    ranges flatMap { case (start, end) =>
      val resultPartition = mutable.ListBuffer[(K, V)]()

      cfor(start)(_ <= end, _ + 1) { index =>
        val path = keyPath(index)
        if(new File(path).exists) {
          val bytes: Array[Byte] = Filesystem.slurp(path)
          val recs = AvroEncoder.fromBinary(writerSchema.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
          resultPartition ++= {
            if(filterIndexOnly)
              recs
            else
              recs.filter { row => includeKey(row._1) }
          }
        }
      }

      resultPartition
    }
  }

  /*Future.sequence(bins.map { partition: Seq[(Long, Long)] => Future {
    val resultPartition = mutable.ListBuffer[(K, V)]()

    for(range <- partition) {
      val (start, end) = range
      cfor(start)(_ <= end, _ + 1) { index =>
        val path = keyPath(index)
        if(new File(path).exists) {
          val bytes: Array[Byte] = Filesystem.slurp(path)
          val recs = AvroEncoder.fromBinary(writerSchema.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
          resultPartition ++= {
            if(filterIndexOnly)
              recs
            else
              recs.filter { row => includeKey(row._1) }
          }
        }
      }
    }

    resultPartition.iterator
  } })*/


}
}
