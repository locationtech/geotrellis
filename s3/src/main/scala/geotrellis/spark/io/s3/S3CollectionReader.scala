package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io.CollectionLayerReader
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
import scalaz.std.vector._
import scalaz.concurrent.Task
import scalaz.stream.{Process, nondeterminism}

import java.util.concurrent.Executors

trait S3CollectionReader {

  def getS3Client: () => S3Client

  def read[
    K: AvroRecordCodec: Boundable,
    V: AvroRecordCodec
  ](
     bucket: String,
     keyPath: Long => String,
     queryKeyBounds: Seq[KeyBounds[K]],
     decomposeBounds: KeyBounds[K] => Seq[(Long, Long)],
     filterIndexOnly: Boolean,
     writerSchema: Option[Schema] = None,
     numPartitions: Option[Int] = None
   ): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(CollectionLayerReader.defaultNumPartitions)).toVector.map(_.toIterator)

    val includeKey = (key: K) => queryKeyBounds.includeKey(key)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val _getS3Client = getS3Client
    val s3client = _getS3Client()

    val pool = Executors.newFixedThreadPool(8)

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
            val getS3Bytes = () => IOUtils.toByteArray(s3client.getObject(bucket, path).getObjectContent)

            try {
              val bytes: Array[Byte] = getS3Bytes()
              val recs = AvroEncoder.fromBinary(writerSchema.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
              if (filterIndexOnly) Some(recs, iter)
              else Some(recs.filter { row => includeKey(row._1) }, iter)
            } catch {
              case e: AmazonS3Exception if e.getStatusCode == 404 => Some(Vector.empty, iter)
            }
          } else {
            None
          }
        }
      }

      nondeterminism.njoin(maxOpen = 8, maxQueued = 8) { range map read }.runFoldMap(identity).unsafePerformSync
    }

    pool.shutdown(); result
  }
}

object S3CollectionReader extends S3CollectionReader {
  def getS3Client: () => S3Client = () => S3Client.default
}
