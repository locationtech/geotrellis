package geotrellis.spark.io.s3

import com.amazonaws.services.s3.model.ObjectListing
import geotrellis.spark.LayerId
import geotrellis.spark.io._
import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._
import scala.collection.JavaConversions._

class S3LayerCopier
  (attributeStore: AttributeStore[JsonFormat],
       destBucket: String,
    destKeyPrefix: String) extends LayerCopier[LayerId] {

  def getS3Client: () => S3Client = () => S3Client.default

  def copyListing(s3Client: S3Client, bucket: String, listing: ObjectListing, from: LayerId, to: LayerId): Unit = {
    listing.getObjectSummaries.foreach { os =>
      val key = os.getKey
      s3Client.copyObject(bucket, key, destBucket, key.replace(s"${from.name}/${from.zoom}", s"${to.name}/${to.zoom}"))
    }
    if (listing.isTruncated) copyListing(s3Client, bucket, s3Client.listNextBatchOfObjects(listing), from, to)
  }

  def copy(from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    val (header, _, _, _, _) = try {
      attributeStore.readLayerAttributes[S3LayerHeader, Unit, Unit, Unit, Unit](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerDeleteError(from).initCause(e)
    }

    val bucket = header.bucket
    val prefix = header.key
    val s3Client = getS3Client()

    copyListing(s3Client, bucket, s3Client.listObjects(bucket, prefix), from, to)
    attributeStore.copy(from, to)
  }
}

object S3LayerCopier {
  def apply
  (attributeStore: AttributeStore[JsonFormat],
       destBucket: String,
    destKeyPrefix: String): S3LayerCopier =
    new S3LayerCopier(attributeStore, destBucket, destKeyPrefix)

  def apply
  (bucket: String, keyPrefix: String, destBucket: String, destKeyPrefix: String): S3LayerCopier =
    apply(S3AttributeStore(bucket, keyPrefix), destBucket, destKeyPrefix)

  def apply
  (bucket: String, keyPrefix: String): S3LayerCopier =
    apply(S3AttributeStore(bucket, keyPrefix), bucket, keyPrefix)
}
