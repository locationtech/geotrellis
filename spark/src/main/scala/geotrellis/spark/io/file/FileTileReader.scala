package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index._
import geotrellis.spark.io.avro._
import geotrellis.util.Filesystem

import org.apache.avro.Schema
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.io.File
import scala.reflect.ClassTag

class FileTileReader[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](
  val attributeStore: AttributeStore[JsonFormat],
  catalogPath: String
)  extends Reader[LayerId, Reader[K, V]] {

  def read(layerId: LayerId): Reader[K, V] = new Reader[K, V] {

    val (layerMetadata, _, keyIndex, writerSchema) =
      attributeStore.readLayerAttributes[FileLayerHeader, Unit, KeyIndex[K], Schema](layerId)

    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = KeyPathGenerator(catalogPath, layerMetadata.path, keyIndex, maxWidth)

    def read(key: K): V = {
      val path = keyPath(key)

      if(!new File(path).exists)
        throw new TileNotFoundError(key, layerId)

      val bytes = Filesystem.slurp(path)
      val recs = AvroEncoder.fromBinary(writerSchema, bytes)(KeyValueRecordCodec[K, V])

      recs
        .find { case (recordKey, _) => recordKey == key }
        .map { case (_, recordValue) => recordValue }
        .getOrElse(throw new TileNotFoundError(key, layerId))
    }
  }
}

object FileTileReader {
  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](catalogPath: String): FileTileReader[K, V] =
    new FileTileReader[K, V](new FileAttributeStore(catalogPath), catalogPath)

  def apply[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](attributeStore: FileAttributeStore): FileTileReader[K, V] =
    new FileTileReader[K, V](attributeStore, attributeStore.catalogPath)
}
