package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.utils.KryoSerializer
import org.apache.spark.SparkContext
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.s3.model.{ListObjectsRequest, ObjectListing}
import com.typesafe.scalalogging.slf4j._
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
import com.amazonaws.auth.{DefaultAWSCredentialsProviderChain, AWSCredentialsProvider}

abstract class RasterRDDReader[K: Boundable: ClassTag] extends LazyLogging {
  import RasterRDDReader._

  /** Converting lower bound of Range to first Key for Marker */
  val indexToPath: (Long, Int) => String

  /* Convert key to index to know if we have rached the range end */
  val pathToIndex: String => Long 
  
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
    logger.debug(s"Loading $layerId from $dir")
    
    val ranges = queryKeyBounds.map{ keyIndex.indexRanges(_) }.flatten
    val bins = balancedBin(ranges, numPartitions)
    logger.info(s"Created ${ranges.length} ranges, binned into ${bins.length} bins")

    // Broadcast the functions that translate from a path to an index and back
    val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
    val BC = sc.broadcast(s3client, pathToIndex, 
      { (index: Long) => indexToPath(index, maxWidth) },
      { (key: K) => queryKeyBounds.includeKey(key) })

    val rdd =
      sc
        .parallelize(bins, bins.size)
        .mapPartitions { rangeList =>
          val (fS3client, toIndex, toPath, includeKey) = BC.value
          val s3client = fS3client()
          
          rangeList
            .flatMap(ranges => ranges)
            .flatMap { range =>
              listKeys(s3client, bucket, dir, range, toPath, toIndex)
                .map { path =>
                  val is = s3client.getObject(bucket, path).getObjectContent
                  KryoSerializer.deserializeStream[(K, Tile)](is)
                 }
                .filter{ 
                  row => includeKey(row._1) 
                }
            }
        }

    new RasterRDD(rdd, rasterMetaData)
  }
}

object RasterRDDReader extends LazyLogging {
  /**
   * Returns a list of keys for given SFC range. Will skip foroward to first possible key
   * and keep paging until reaching the last key is seen.
   */  
  def listKeys(
    s3client: S3Client, 
    bucket: String, key: String, 
    range: (Long, Long), 
    indexToPath: Long => String, 
    pathToIndex: String => Long
  ): Vector[String] = {
    val (minIndex, maxIndex) = range
    val delim = "/"
    val request = new ListObjectsRequest()
      .withBucketName(bucket)
      .withPrefix(key + "/")
      .withDelimiter(delim)
      .withMaxKeys(math.min((maxIndex - minIndex).toInt + 1, 1024))
      .withMarker(key + "/" + indexToPath(minIndex - 1))

    def readKeys(keys: Seq[String]): (Seq[String], Boolean) = {
      val ret = ArrayBuffer.empty[String]   
      var endSeen = false
      keys.foreach{ key => 
        val index = pathToIndex(key)

        if (index >= minIndex && index <= maxIndex)
          ret += key
        if (index > maxIndex)
          endSeen = true
      }
      (ret, endSeen)
    }

    var listing: ObjectListing = null
    var foundKeys = Vector.empty[String]
    var stop = false
    do {
      listing = s3client.listObjects(request)
      // the keys could be either incomplete or include extra information, but in order.
      // need to decides if I ask for more or truncate the result      
      val (pairs, endSeen) = readKeys(listing.getObjectSummaries.asScala.map(_.getKey))
      foundKeys = foundKeys ++ pairs
      stop = endSeen
      request.setMarker(listing.getNextMarker)
    } while (listing.isTruncated && !stop)

    foundKeys
  }

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
    while (! stack.isEmpty) {
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
