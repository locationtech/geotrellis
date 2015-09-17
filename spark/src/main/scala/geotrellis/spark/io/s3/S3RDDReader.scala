package geotrellis.spark.io.s3

import com.amazonaws.services.s3.model.AmazonS3Exception
import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark._
import geotrellis.spark.io.Cache
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.avro.{AvroEncoder, AvroRecordCodec}
import geotrellis.spark.utils.KryoWrapper
import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

class S3RDDReader[K: Boundable: AvroRecordCodec: ClassTag, V: AvroRecordCodec: ClassTag](bucket: String, getS3Client: () => S3Client)(implicit sc: SparkContext) {

  def read(
    queryKeyBounds: Seq[KeyBounds[K]],
    keyIndex: KeyIndex[K],
    keyPath: Long => String,
    writerSchema: Schema,
    numPartitions: Int,
    cache: Option[Cache[Long, Array[Byte]]] = None
  ): RDD[(K, V)] = {
    val bucket = this.bucket
    val ranges = queryKeyBounds.flatMap(keyIndex.indexRanges(_))
    val bins = S3RDDReader.balancedBin(ranges, numPartitions)
    val recordCodec = KeyValueRecordCodec[K, V]
    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val _getS3Client = getS3Client

    val BC = KryoWrapper((_getS3Client, recordCodec, writerSchema, cache))

    val rdd =
      sc.parallelize(bins, bins.size)
        .mapPartitions { partition: Iterator[Seq[(Long, Long)]] =>
          val (getS3Client, recCodec, schema, cache) = BC.value
          val s3client = getS3Client()

          val tileSeq: Iterator[Seq[(K, V)]] =
            for{
              rangeList <- partition // Unpack the one element of this partition, the rangeList.
              range <- rangeList
              index <- range._1 to range._2
            } yield {
              val path = keyPath(index)
              val getS3Bytes = () => IOUtils.toByteArray(s3client.getObject(bucket, path).getObjectContent)

              try {
                val bytes: Array[Byte] =
                  cache match {
                    case Some(cache) =>
                      cache(index).getOrElse {
                        val s3Bytes = getS3Bytes()
                        cache.update(index, s3Bytes)
                        s3Bytes
                      }
                    case None =>
                      getS3Bytes()
                  }
                val recs = AvroEncoder.fromBinary(schema, bytes)(recCodec)
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

object S3RDDReader extends LazyLogging {
  /**
   * Will attempt to bin ranges into buckets, each containing at least the average number of elements.
   * Trailing bins may be empty if the count is too high for number of ranges.
   */
  def balancedBin(ranges: Seq[(Long, Long)], count: Int ): Seq[Seq[(Long, Long)]] = {
    var stack = ranges.toList

    def len(r: (Long, Long)) = r._2 - r._1 + 1l
    val total = ranges.foldLeft(0l){ (s,r) => s + len(r) }
    val binWidth = total / count + 1

    def splitRange(range: (Long, Long), take: Long): ((Long, Long), (Long, Long)) = {
      assert(len(range) > take)
      (range._1, range._1 + take - 1) -> (range._1 + take, range._2)
    }

    val arr = Array.fill(count)(Nil: List[(Long, Long)])
    var sum = 0l
    var i = 0
    while (stack.nonEmpty) {
      if (len(stack.head) + sum <= binWidth){
        val take = stack.head
        arr(i) = take :: arr(i)
        sum += len(take)
        stack = stack.tail
      }else{
        val (take, left) = splitRange(stack.head, binWidth - sum)
        stack = left :: stack.tail
        arr(i) = take :: arr(i)
        sum += len(take)
      }

      if (sum >= binWidth) {
        sum = 0l
        i += 1
      }
    }
    arr
  }
}