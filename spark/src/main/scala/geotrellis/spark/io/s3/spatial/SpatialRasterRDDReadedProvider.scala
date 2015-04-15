package geotrellis.spark.io.s3.spatial

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.spark.io._

import geotrellis.spark.io.s3._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import com.typesafe.scalalogging.slf4j._
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ListObjectsRequest, ObjectListing}
import geotrellis.index.zcurve._
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import com.amazonaws.auth.{AWSCredentialsProvider, BasicAWSCredentials}

/**
 * Notes:
 * Right now I am decomposing ranges and sending them off to be listed by the clients.
 * The downside is that I lose information, ranges could be empty if the raster is sparse.
 * Would it be more efficient to list the ranges on the driver and ship the keys?
 */
object SpatialRasterRDDReaderProvider extends RasterRDDReaderProvider[SpatialKey] with LazyLogging {
  def reader(credentialsProvider: AWSCredentialsProvider,layerMetaData: S3LayerMetaData)(implicit sc: SparkContext): FilterableRasterRDDReader[SpatialKey] =
    new FilterableRasterRDDReader[SpatialKey] {
      def read(layerId: LayerId, filterSet: FilterSet[SpatialKey]): RasterRDD[SpatialKey] = {        
        val bucket = layerMetaData.bucket
        val dir = layerMetaData.key
        val rasterMetaData = layerMetaData.rasterMetaData
        logger.debug(s"Loading $layerId from $dir")

        // pull out spatial filters and decompose them into index ranges
        var ranges = 
          (for (filter <- filterSet.filters) yield {
            filter match {
              case SpaceFilter(b) =>
                Z2.zranges(Z2(b.colMin, b.rowMin), Z2(b.colMax, b.rowMax))
            }
          }).flatten

        // if we have no filter, decompose my raster extent
        if (ranges.isEmpty) {
          val b = rasterMetaData.gridBounds
          ranges = Z2.zranges(Z2(b.colMin, b.rowMin), Z2(b.colMax, b.rowMax))
        }

        val bins = ballancedBin(ranges, sc.defaultParallelism)
        logger.info(s"Created ${ranges.length} ranges, binned into ${bins.length} bins")

        val bcCredentials = sc.broadcast(credentialsProvider.getCredentials)

        val rdd: RDD[(SpatialKey, Tile)] = sc
          .parallelize(bins)
          .mapPartitions{ rangeList =>
            val s3Client = new AmazonS3Client(bcCredentials.value)
            
            rangeList
              .flatMap( ranges => ranges)            
              .flatMap{ range => 
                listKeys(s3Client, bucket, dir, range) 
              }
              .map{ case (key, path) =>
                val is = s3Client.getObject(bucket, path).getObjectContent
                val tile =
                  ArrayTile.fromBytes(
                    S3RecordReader.readInputStream(is),
                    rasterMetaData.cellType,
                    rasterMetaData.tileLayout.tileCols,
                    rasterMetaData.tileLayout.tileRows
                  )
                is.close()
                key -> tile     
              }
          }
        new RasterRDD(rdd, rasterMetaData)
      }
    }

  /* This needs to happen, in space-time case we will not be able to enumarate the keys */
  private def listKeys(s3client: AmazonS3Client, bucket: String, key: String, range: (Long, Long)): Vector[(SpatialKey, String)] = {
    logger.debug(s"Listing: $key, $range")
    
    val (minKey, maxKey) = range
    val delim = "/"        
    val request = new ListObjectsRequest()
      .withBucketName(bucket)
      .withPrefix(key + "/")
      .withDelimiter(delim)
      .withMaxKeys(math.min((maxKey - minKey).toInt+1, 1024))
      .withMarker(key + "/" + f"${minKey - 1}%019d")
    
    val tileIdRx = """.+\/(\d+)""".r
    def readKeys(keys: Seq[String]): (Seq[(SpatialKey, String)], Boolean) = {
      val ret = ArrayBuffer.empty[(SpatialKey, String)]
      var endSeen = false
      keys.foreach{ key => 
        val tileIdRx(tileId) = key
        val index = new Z2(tileId.toLong)
        if (index.z >= minKey && index.z <= maxKey)           
          ret += SpatialKey(index.dim(0), index.dim(1)) -> key        
        if (index.z >= maxKey)
          endSeen = true        
      }
      ret -> endSeen
    }

    var listing: ObjectListing = null
    var foundKeys: Vector[(SpatialKey, String)] = Vector.empty    
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
  def ballancedBin(ranges: Seq[(Long, Long)], count: Int ): Seq[Seq[(Long, Long)]] = {
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
