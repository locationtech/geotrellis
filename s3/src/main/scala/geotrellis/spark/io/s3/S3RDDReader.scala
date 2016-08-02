package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.{IndexRanges, MergeQueue}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.util.KryoWrapper

import scalaz.concurrent.Task
import scalaz.stream.{Process, nondeterminism}
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import java.util.concurrent.Executors

trait S3RDDReader {

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
  )(implicit sc: SparkContext): RDD[(K, V)] = {
    if(queryKeyBounds.isEmpty) return sc.emptyRDD[(K, V)]

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(sc.defaultParallelism))

    val includeKey = (key: K) => queryKeyBounds.includeKey(key)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val _getS3Client = getS3Client
    val kwWriterSchema = KryoWrapper(writerSchema) //Avro Schema is not Serializable

    val rdd =
      sc.parallelize(bins, bins.size)
        .mapPartitions { partition: Iterator[Seq[(Long, Long)]] =>
          val s3client = _getS3Client()
          val pool = Executors.newFixedThreadPool(8)

          val result = partition flatMap { range =>
            val ranges = Process.unfold(range.toIterator) { iter: Iterator[(Long, Long)] =>
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
                      val recs = AvroEncoder.fromBinary(kwWriterSchema.value.getOrElse(_recordCodec.schema), bytes)(_recordCodec)
                      if(filterIndexOnly)
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

    rdd
  }
}

object S3RDDReader extends S3RDDReader {
  def getS3Client: () => S3Client = () => S3Client.default
}
