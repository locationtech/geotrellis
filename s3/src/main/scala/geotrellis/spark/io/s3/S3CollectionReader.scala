package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.{MergeQueue, IndexRanges}
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
import org.apache.spark.SparkContext

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

    // val bins = IndexRanges.bin(ranges, numPartitions.getOrElse(sc.defaultParallelism))

    val includeKey = (key: K) => queryKeyBounds.includeKey(key)
    val _recordCodec = KeyValueRecordCodec[K, V]
    val _getS3Client = getS3Client
    val s3client = _getS3Client()

    (for {
      range <- ranges
      index <- range._1 to range._2
    } yield {
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
    }).flatten
  }
}

object S3CollectionReader extends S3CollectionReader {
  def getS3Client: () => S3Client = () => S3Client.default
}
