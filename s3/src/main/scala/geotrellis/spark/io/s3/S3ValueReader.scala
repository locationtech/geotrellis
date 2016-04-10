package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index._

import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect.ClassTag

class S3ValueReader(
  val attributeStore: AttributeStore
) extends ValueReader[LayerId] {

  val s3Client: S3Client = S3Client.default

  def reader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](layerId: LayerId): Reader[K, V] = new Reader[K, V] {
    val header = attributeStore.readHeader[S3LayerHeader](layerId)
    val keyIndex = attributeStore.readKeyIndex[K](layerId)
    val writerSchema = attributeStore.readSchema(layerId)


    def read(key: K): V = {
      val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
      val path = s"${header.key}/${Index.encode(keyIndex.toIndex(key), maxWidth)}"

      val is =
        try {
          s3Client.getObject(header.bucket, path).getObjectContent
        } catch {
          case e: AmazonS3Exception if e.getStatusCode == 404 =>
            throw new TileNotFoundError(key, layerId)
        }

      val bytes = IOUtils.toByteArray(is)
      val recs = AvroEncoder.fromBinary(writerSchema, bytes)(KeyValueRecordCodec[K, V])

      recs
        .find { row => row._1 == key }
        .map { row => row._2 }
        .getOrElse(throw new TileNotFoundError(key, layerId))
    }
  }
}

object S3ValueReader {
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](
    attributeStore: AttributeStore,
    layerId: LayerId
  ): Reader[K, V] =
    new S3ValueReader(attributeStore).reader[K, V](layerId)

  def apply(bucket: String, root: String): S3ValueReader =
    new S3ValueReader(new S3AttributeStore(bucket, root))

  def apply(bucket: String): S3ValueReader =
    apply(bucket, "")
}
