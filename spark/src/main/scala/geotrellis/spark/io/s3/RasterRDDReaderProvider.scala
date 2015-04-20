package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.utils.KryoSerializer
import org.apache.spark.SparkContext
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ListObjectsRequest, ObjectListing}
import com.typesafe.scalalogging.slf4j._
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

abstract class RasterRDDReaderProvider[K: ClassTag] extends LazyLogging {
  def setFilters(filterSet: FilterSet[K], kb: KeyBounds[K], ki: KeyIndex[K]): Seq[(Long, Long)]

  def reader(
    credentialsProvider: AWSCredentialsProvider,
    layerMetaData: S3LayerMetaData,
    keyBounds: KeyBounds[K],
    keyIndex: KeyIndex[K])
  (implicit sc: SparkContext): FilterableRasterRDDReader[K] = 
    new FilterableRasterRDDReader[K] {
      def read(layerId: LayerId, filterSet: FilterSet[K]): RasterRDD[K] = {        
        val bucket = layerMetaData.bucket
        val dir = layerMetaData.key
        val rasterMetaData = layerMetaData.rasterMetaData
        logger.debug(s"Loading $layerId from $dir")

        var ranges = setFilters(filterSet, keyBounds, keyIndex)
        val bins = ballancedBin(ranges, sc.defaultParallelism)
        logger.info(s"Created ${ranges.length} ranges, binned into ${bins.length} bins")

        val bcCredentials = sc.broadcast(credentialsProvider.getCredentials)

        val rdd = 
          sc.parallelize(bins)
          .mapPartitions{ rangeList =>
            val s3Client = new AmazonS3Client(bcCredentials.value)
            
            rangeList
              .flatMap( ranges => ranges)            
              .flatMap{ range => 
                listKeys(s3Client, bucket, dir, range) 
              }
              .map{ path =>
                val is = s3Client.getObject(bucket, path).getObjectContent
                KryoSerializer.deserializeStream[(K, Tile)](is)
              }
          }
        new RasterRDD(rdd, rasterMetaData)
      }
    }

  /**
   * Returns a list of keys for given SFC range. Will skip foroward to first possible key
   * and keep paging until reaching the last key is seen.
   */
  def listKeys(s3client: AmazonS3Client, bucket: String, key: String, range: (Long, Long)): Vector[String] = {
    logger.debug(s"Listing: $key, $range")
    val tileIdRx = """.+\/(\d+)""".r    
    
    def indexToPath(i: Long): String = 
      f"${i}%019d"
        
    def pathToIndex(s: String): Long = {
      val tileIdRx(tileId) = s
      tileId.toLong
    }

    val (minKey, maxKey) = range
    val delim = "/"        
    val request = new ListObjectsRequest()
      .withBucketName(bucket)
      .withPrefix(key + "/")
      .withDelimiter(delim)
      .withMaxKeys(math.min((maxKey - minKey).toInt+1, 1024))
      .withMarker(key + "/" + indexToPath(minKey - 1))
    
    def readKeys(keys: Seq[String]): (Seq[String], Boolean) = {
      val ret = ArrayBuffer.empty[String]   
      var endSeen = false
      keys.foreach{ key => 
        val index = pathToIndex(key)
        if (index >= minKey && index <= maxKey)           
          ret += key        
        if (index >= maxKey)
          endSeen = true        
      }
      ret -> endSeen
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
   * Will attemp to bin ranges into buckets, each containing at least the average number of elements.
   * Trailing bins may be empty if the count is too high for number of ranges.
   */
  protected def ballancedBin(ranges: Seq[(Long, Long)], count: Int ): Seq[Seq[(Long, Long)]] = {
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
      if (len(stack.head)+sum <= binWidth){
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