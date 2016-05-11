package geotrellis.spark.io.s3

import geotrellis.spark.LayerId
import geotrellis.spark.io._
import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._

class S3LayerDeleter(val attributeStore: AttributeStore) extends LayerDeleter[LayerId] {

  def getS3Client: () => S3Client = () => S3Client.default

  def delete(id: LayerId): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val header = try {
      attributeStore.readHeader[S3LayerHeader](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerDeleteError(id).initCause(e)
    }

    val bucket = header.bucket
    val prefix = header.key
    val s3Client = getS3Client()

    s3Client.deleteListing(bucket, s3Client.listObjects(bucket, prefix))
    attributeStore.delete(id)
  }
}

object S3LayerDeleter {
  def apply(attributeStore: AttributeStore): S3LayerDeleter = new S3LayerDeleter(attributeStore)

  def apply(bucket: String, prefix: String): S3LayerDeleter = apply(S3AttributeStore(bucket, prefix))
}
