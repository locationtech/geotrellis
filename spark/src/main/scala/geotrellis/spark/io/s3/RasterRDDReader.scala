package geotrellis.spark.io.s3

import com.amazonaws.services.s3.model.AmazonS3Exception
import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.json._
import geotrellis.spark.io.{FilteringRasterRDDReader, AttributeCaching}
import geotrellis.spark.io.avro.{AvroEncoder, KeyValueRecordCodec, AvroRecordCodec}
import geotrellis.spark.utils.KryoWrapper
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import spray.json.{JsObject, JsonFormat}
import spray.json.DefaultJsonProtocol._

import scala.reflect.ClassTag

class RasterRDDReader[K: SpatialComponent: Boundable: AvroRecordCodec: JsonFormat: ClassTag]
(val attributeStore: S3AttributeStore)(implicit sc: SparkContext)
  extends FilteringRasterRDDReader[K] with AttributeCaching[S3LayerMetaData] with LazyLogging {

  val getS3Client: () => S3Client = () => S3Client.default
  val defaultNumPartitions = sc.defaultParallelism

  def read(id: LayerId, rasterQuery: RasterRDDQuery[K], numPartitions: Int): RasterRDD[K] = {
    val metadata  = getLayerMetadata(id)
    val keyBounds = getLayerKeyBounds[K](id)
    val keyIndex  = getLayerKeyIndex[K](id)

    val bucket = metadata.bucket
    val prefix = metadata.key

    val rasterMetadata = metadata.rasterMetaData
    val queryKeyBounds = rasterQuery(rasterMetadata, keyBounds)
    val ranges = queryKeyBounds.flatMap(keyIndex.indexRanges(_))
    val bins = RasterRDDReader.balancedBin(ranges, numPartitions)

    logger.debug(s"Loading layer from $bucket $prefix, ${ranges.length} ranges split into ${bins.length} bins")

    val writerSchema: Schema = (new Schema.Parser).parse(attributeStore.read[JsObject](id, "schema").toString())
    val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
    val recordCodec = KeyValueRecordCodec[K, Tile]
    val boundable = implicitly[Boundable[K]]
    val includeKey = (key: K) => KeyBounds.includeKey(queryKeyBounds, key)(boundable)
    val toPath = (index: Long) => encodeIndex(index, maxWidth)

    val BC = KryoWrapper((getS3Client, recordCodec, writerSchema))

    val rdd =
      sc.parallelize(bins, bins.size)
        .mapPartitions { partition: Iterator[Seq[(Long, Long)]] =>
          val (getS3Client, recCodec, schema) = BC.value
          val s3client = getS3Client()

          val tileSeq: Iterator[Seq[(K, Tile)]] =
            for{
              rangeList <- partition // Unpack the one element of this partition, the rangeList.
              range <- rangeList
              index <- range._1 to range._2
            } yield {
              val path = makePath(prefix, toPath(index))

              try {
                val is = s3client.getObject(bucket, path).getObjectContent
                val bytes = org.apache.commons.io.IOUtils.toByteArray(is)
                val recs = AvroEncoder.fromBinary(schema, bytes)(recCodec)
                recs.filter { row => includeKey(row._1) }
              } catch {
                case e: AmazonS3Exception if e.getStatusCode == 404 => Seq.empty
              }
            }

          tileSeq.flatten
        }

    new RasterRDD[K](rdd, rasterMetadata)
  }
}

object RasterRDDReader extends LazyLogging {
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