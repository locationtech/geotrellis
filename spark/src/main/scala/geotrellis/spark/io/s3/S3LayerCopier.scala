package geotrellis.spark.io.s3

import geotrellis.spark.{Boundable, KeyBounds, LayerId}
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import spray.json.JsonFormat
import geotrellis.spark.io.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect.ClassTag

class S3LayerCopier[K: Boundable: JsonFormat: ClassTag]
  (attributeStore: AttributeStore[JsonFormat],
       destBucket: String,
    destKeyPrefix: String)(implicit sc: SparkContext) extends LayerCopier[LayerId] {

  def getS3Client: () => S3Client = () => S3Client.default

  def copy(from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    val (header, _, keyBounds, keyIndex, _) = try {
      attributeStore.readLayerAttributes[S3LayerHeader, Unit, KeyBounds[K], KeyIndex[K], Unit](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerDeleteError(from).initCause(e)
    }

    val bucket = header.bucket
    val prefix = header.key
    val destPrefix = makePath(destKeyPrefix, s"${to.name}/${to.zoom}")
    val numPartitions = 1
    val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
    val keyPath = (index: Long) => makePath(prefix, encodeIndex(index, maxWidth))
    val destKeyPath = (index: Long) => makePath(destPrefix, encodeIndex(index, maxWidth))
    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
    val ranges = decompose(keyBounds)
    val bins = S3RDDReader.balancedBin(ranges, numPartitions)

    sc.parallelize(bins, bins.size)
      .mapPartitions { partition: Iterator[Seq[(Long, Long)]] =>
        val s3client = getS3Client()
        for {
          rangeList <- partition
          range <- rangeList
          index <- range._1 to range._2
        } yield {
          s3client.copyObject(bucket, keyPath(index), destBucket, destKeyPath(index))
        }
      }

    attributeStore.copy(from, to)
  }
}

object S3LayerCopier {
  def apply[K: Boundable: JsonFormat: ClassTag]
  (attributeStore: AttributeStore[JsonFormat],
       destBucket: String,
    destKeyPrefix: String)(implicit sc: SparkContext): S3LayerCopier[K] =
    new S3LayerCopier[K](attributeStore, destBucket, destKeyPrefix)

  def apply[K: Boundable: JsonFormat: ClassTag]
  (bucket: String, keyPrefix: String, destBucket: String, destKeyPrefix: String)(implicit sc: SparkContext): S3LayerCopier[K] =
    apply[K](S3AttributeStore(bucket, keyPrefix), destBucket, destKeyPrefix)

  def apply[K: Boundable: JsonFormat: ClassTag]
  (bucket: String, keyPrefix: String)(implicit sc: SparkContext): S3LayerCopier[K] =
    apply[K](S3AttributeStore(bucket, keyPrefix), bucket, keyPrefix)
}
