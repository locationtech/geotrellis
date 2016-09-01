package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.MergeQueue
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
import com.typesafe.config.ConfigFactory

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
     threads: Int = ConfigFactory.load().getThreads("geotrellis.s3.threads.collection.read")
   ): Seq[(K, V)] = {
    if (queryKeyBounds.isEmpty) return Seq.empty[(K, V)]

    val ranges = if (queryKeyBounds.length > 1)
      MergeQueue(queryKeyBounds.flatMap(decomposeBounds))
    else
      queryKeyBounds.flatMap(decomposeBounds)

    val recordCodec = KeyValueRecordCodec[K, V]
    val s3client = getS3Client()

    LayerReader.njoin[K, V](ranges.toIterator, threads){ index: Long =>
      try {
        val bytes = IOUtils.toByteArray(s3client.getObject(bucket, keyPath(index)).getObjectContent)
        val recs = AvroEncoder.fromBinary(writerSchema.getOrElse(recordCodec.schema), bytes)(recordCodec)
        if (filterIndexOnly) recs
        else recs.filter { row => queryKeyBounds.includeKey(row._1) }
      } catch {
        case e: AmazonS3Exception if e.getStatusCode == 404 => Vector.empty
      }
    }: Seq[(K, V)]
  }
}

object S3CollectionReader extends S3CollectionReader {
  def getS3Client: () => S3Client = () => S3Client.default
}
