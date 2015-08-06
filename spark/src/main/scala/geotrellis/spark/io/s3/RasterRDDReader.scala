package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import org.apache.spark.SparkContext
import com.typesafe.scalalogging.slf4j._
import scala.reflect.ClassTag
import geotrellis.spark.io.avro._

class RasterRDDReader[K: AvroRecordCodec: Boundable: ClassTag] extends LazyLogging {

  /** Converting lower bound of Range to first Key for Marker */
  val indexToPath: (Long, Int) => String = encodeIndex

  def read(
    s3client: ()=>S3Client,
    layerMetaData: S3LayerMetaData,
    keyBounds: KeyBounds[K],
    keyIndex: KeyIndex[K],
    numPartitions: Int
  )
  (layerId: LayerId, queryKeyBounds: Seq[KeyBounds[K]])
  (implicit sc: SparkContext): RasterRDD[K] = {
    val bucket = layerMetaData.bucket
    val dir = layerMetaData.key
    val rasterMetaData = layerMetaData.rasterMetaData

    // TODO: now that we don't have to LIST, can we prefix key with a hash to get better throughput ?
    val ranges = queryKeyBounds.flatMap(keyIndex.indexRanges(_))
    val bins = RasterRDDReader.balancedBin(ranges, numPartitions)
    logger.debug(s"Loading layer from $bucket $dir, ${ranges.length} ranges split into ${bins.length} bins")

    // Broadcast the functions and objects we can use in the closure
    val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
    val BC = sc.broadcast((
      s3client,
      { (index: Long) => indexToPath(index, maxWidth) },
      { (key: K) => queryKeyBounds.includeKey(key) },
      geotrellis.spark.io.avro.recordCodec(implicitly[AvroRecordCodec[K]],
        geotrellis.spark.io.avro.tileUnionCodec)
    ))

    val rdd =
      sc
        .parallelize(bins, bins.size)
        .mapPartitions { rangeList =>
          val (fS3client, toPath, includeKey, recCodec) = BC.value
          val s3client = fS3client()

          rangeList
            .flatMap(ranges => ranges)
            .flatMap { range =>
              {for (index <- range._1 to range._2) yield {
                val path = List(dir, toPath(index)).filter(_.nonEmpty).mkString("/")
                val is = s3client.getObject(bucket, path).getObjectContent
                val bytes = org.apache.commons.io.IOUtils.toByteArray(is)
                val recs = AvroEncoder.fromBinary(bytes)(recCodec)
                recs
                  .filter( row => includeKey(row._1) )
                  .map { case (key, tile) => key -> tile }
              }}.flatten
            }
        }

      new RasterRDD[K](rdd, rasterMetaData)
  }
}

object RasterRDDReader extends LazyLogging {
  /**
   * Will attempt to bin ranges into buckets, each containing at least the average number of elements.
   * Trailing bins may be empty if the count is too high for number of ranges.
   */
  protected def balancedBin(ranges: Seq[(Long, Long)], count: Int ): Seq[Seq[(Long, Long)]] = {
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
