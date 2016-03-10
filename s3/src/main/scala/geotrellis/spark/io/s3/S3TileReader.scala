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

class S3TileReader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](
  val attributeStore: AttributeStore[JsonFormat]
)  extends Reader[LayerId, Reader[K, V]] {

  val s3Client: S3Client = S3Client.default

  def read(layerId: LayerId): Reader[K, V] = new Reader[K, V] {

    val (layerMetadata, _, keyIndex, writerSchema) =
      attributeStore.readLayerAttributes[S3LayerHeader, Unit, KeyIndex[K], Schema](layerId)

    def read(key: K): V = {
      val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
      val path = s"${layerMetadata.key}/${Index.encode(keyIndex.toIndex(key), maxWidth)}"

      val is =
        try {
          s3Client.getObject(layerMetadata.bucket, path).getObjectContent
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

object S3TileReader {
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](bucket: String, root: String): S3TileReader[K, V] =
    new S3TileReader[K, V](new S3AttributeStore(bucket, root))

  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](bucket: String): S3TileReader[K, V] =
    apply(bucket, "")
}
