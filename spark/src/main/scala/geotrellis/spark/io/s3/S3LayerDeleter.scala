package geotrellis.spark.io.s3

import geotrellis.spark.{Boundable, KeyBounds, LayerId}
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import spray.json.JsonFormat
import geotrellis.spark.io.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect.ClassTag

class S3LayerDeleter[K: Boundable: JsonFormat: ClassTag]
  (attributeStore: AttributeStore[JsonFormat])(implicit sc: SparkContext) extends LayerDeleter[K, LayerId] {

  def getS3Client: () => S3Client = () => S3Client.default

  def delete(id: LayerId): Unit = {
    try {
      if(!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
      val (header, _, keyBounds, keyIndex, _) =
        attributeStore.readLayerAttributes[S3LayerHeader, Unit, KeyBounds[K], KeyIndex[K], Unit](id)

      val bucket = header.bucket
      val prefix = header.key
      val numPartitions = 1
      val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
      val keyPath = (index: Long) => makePath(prefix, encodeIndex(index, maxWidth))
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
            s3client.deleteObject(bucket, keyPath(index))
          }
        }

      attributeStore.delete(id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerDeleteError(id).initCause(e)
    }
  }
}

object S3LayerDeleter {
  def apply[K: Boundable: JsonFormat: ClassTag](bucket: String, prefix: String)(implicit sc: SparkContext) =
    new S3LayerDeleter[K](S3AttributeStore(bucket, prefix))
}
