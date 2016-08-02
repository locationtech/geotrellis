package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io.CollectionLayerReader
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
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

    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(CollectionLayerReader.defaultNumPartitions))

    val includeKey = (key: K) => queryKeyBounds.includeKey(key)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val _getS3Client = getS3Client
    val s3client = _getS3Client()

    val pool = Executors.newFixedThreadPool(8)

    val result = bins flatMap { partition =>
      val ranges = Process.unfold(partition.toIterator) { iter: Iterator[(Long, Long)] =>
        if (iter.hasNext) Some(iter.next(), iter)
        else None
      }

      val read: ((Long, Long)) => Process[Task, List[(K, V)]] = {
        case (start, end) =>
          Process eval {
            Task.gatherUnordered(for {
              index <- start to end
            } yield Task {
              val path = keyPath(index)
              val getS3Bytes = () => IOUtils.toByteArray(s3client.getObject(bucket, path).getObjectContent)

              try {
                val bytes: Array[Byte] =
                  getS3Bytes()
                val recs = AvroEncoder.fromBinary(writerSchema.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
                if (filterIndexOnly)
                  recs
                else
                  recs.filter { row => includeKey(row._1) }
              } catch {
                case e: AmazonS3Exception if e.getStatusCode == 404 => Seq.empty
              }
            }(pool)).map(_.flatten)
          }
      }

      nondeterminism.njoin(maxOpen = 8, maxQueued = 8) { ranges map read }.runLog.map(_.flatten).unsafePerformSync
    }

    pool.shutdown()
    result
  }
}

object S3CollectionReader extends S3CollectionReader {
  def getS3Client: () => S3Client = () => S3Client.default
}
