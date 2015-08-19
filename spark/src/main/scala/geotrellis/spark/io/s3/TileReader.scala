package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.{KeyValueRecordCodec, AvroEncoder, AvroRecordCodec}
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.commons.io.IOUtils
import spray.json.JsonFormat
import scala.reflect.ClassTag

class TileReader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](
  val attributeStore: S3AttributeStore,
  layerId: LayerId
) extends Reader[K, V] with AttributeCaching[S3LayerMetaData] {
  val s3Client: S3Client  = S3Client.default
  val metadata  = getLayerMetadata(layerId)
  val keyBounds = getLayerKeyBounds[K](layerId)
  val index     = getLayerKeyIndex[K](layerId)

  def read(key: K): V = {
    val maxWidth = maxIndexWidth(index.toIndex(keyBounds.maxKey))
    val path = s"${metadata.key}/${encodeIndex(index.toIndex(key), maxWidth)}"

    val is =
      try {
        s3Client.getObject(metadata.bucket, path).getObjectContent
      } catch {
        case e: AmazonS3Exception if e.getStatusCode == 404 =>
          throw new TileNotFoundError(key, layerId)
      }

    val bytes = IOUtils.toByteArray(is)
    val recs = AvroEncoder.fromBinary(bytes)(KeyValueRecordCodec[K, V])

    recs
      .find { row => row._1 == key }
      .map { row => row._2 }
      .getOrElse(throw new TileNotFoundError(key, layerId))
  }
}

object TileReader {
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](bucket: String, root: String, layer: LayerId): TileReader[K, V] =
    new TileReader[K, V](new S3AttributeStore(bucket, root), layer)
  
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](bucket: String, layer: LayerId): TileReader[K, V] =
    apply(bucket, "", layer)
}