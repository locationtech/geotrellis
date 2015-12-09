package geotrellis.spark.io.s3

import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion
import com.amazonaws.services.s3.model.ObjectListing
import geotrellis.spark.LayerId
import geotrellis.spark.io._
import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._
import scala.collection.JavaConversions._

class S3LayerDeleter(attributeStore: AttributeStore[JsonFormat]) extends LayerDeleter[LayerId] {

  def getS3Client: () => S3Client = () => S3Client.default

  def deleteListing(s3Client: S3Client, bucket: String, listing: ObjectListing): Unit = {
    s3Client.deleteObjects(bucket, listing.getObjectSummaries.map { os => new KeyVersion(os.getKey) }.toList)
    if (listing.isTruncated) deleteListing(s3Client, bucket, s3Client.listNextBatchOfObjects(listing))
  }

  def delete(id: LayerId): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val (header, _, _, _, _) = try {
      attributeStore.readLayerAttributes[S3LayerHeader, Unit, Unit, Unit, Unit](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerDeleteError(id).initCause(e)
    }

    val bucket = header.bucket
    val prefix = header.key
    val s3Client = getS3Client()

    deleteListing(s3Client, bucket, s3Client.listObjects(bucket, prefix))
    attributeStore.delete(id)
    attributeStore.clearCache()
  }
}

object S3LayerDeleter {
  def apply(attributeStore: AttributeStore[JsonFormat]) = new S3LayerDeleter(attributeStore)

  def apply(bucket: String, prefix: String) = apply(S3AttributeStore(bucket, prefix))
}
