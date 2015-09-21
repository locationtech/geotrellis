package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.commons.io.IOUtils
import spray.json.JsonFormat
import scala.reflect.ClassTag

class S3TileReader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](
  val attributeStore: S3AttributeStore,
  layerId: LayerId
) extends Reader[K, V] {
  val s3Client: S3Client  = S3Client.default
  val layerMetaData  = attributeStore.cacheRead[S3LayerMetaData](layerId, Fields.layerMetaData)
  val keyBounds = attributeStore.cacheRead[KeyBounds[K]](layerId, Fields.keyBounds)
  val keyIndex  = attributeStore.cacheRead[KeyIndex[K]](layerId, Fields.keyIndex)

  def read(key: K): V = {
    val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
    val path = s"${layerMetaData.key}/${encodeIndex(keyIndex.toIndex(key), maxWidth)}"

    val is =
      try {
        s3Client.getObject(layerMetaData.bucket, path).getObjectContent
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

object S3TileReader {
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](bucket: String, root: String, layer: LayerId): S3TileReader[K, V] =
    new S3TileReader[K, V](new S3AttributeStore(bucket, root), layer)
  
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](bucket: String, layer: LayerId): S3TileReader[K, V] =
    apply(bucket, "", layer)
}