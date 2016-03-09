package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.{MergeQueue, KeyIndex, IndexRanges}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.utils.KryoWrapper
import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.reflect.ClassTag

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

          val tileSeq: Iterator[Seq[(K, V)]] =
            for {
              rangeList <- partition // Unpack the one element of this partition, the rangeList.
              range <- rangeList
              index <- range._1 to range._2
            } yield {
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
            }

          tileSeq.flatten
        }

    rdd
  }
}

object S3RDDReader extends S3RDDReader {
  def getS3Client: () => S3Client = () => S3Client.default
}
