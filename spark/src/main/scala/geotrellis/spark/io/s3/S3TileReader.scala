package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import com.amazonaws.services.s3.model.AmazonS3Exception
import org.apache.avro.Schema
import org.apache.commons.io.IOUtils
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect.ClassTag

class S3TileReader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](
  val attributeStore: S3AttributeStore
)  extends Reader[LayerId, Reader[K, V]] {

  val s3Client: S3Client = S3Client.default

  def read(layerId: LayerId): Reader[K, V] = new Reader[K, V] {

    val (layerMetaData, _, keyBounds, keyIndex, writerSchema) =
      attributeStore.readLayerAttributes[S3LayerHeader, Unit, KeyBounds[K], KeyIndex[K], Schema](layerId)

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
}

object S3TileReader {
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](bucket: String, root: String): S3TileReader[K, V] =
    new S3TileReader[K, V](new S3AttributeStore(bucket, root))
  
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](bucket: String): S3TileReader[K, V] =
    apply(bucket, "")
}