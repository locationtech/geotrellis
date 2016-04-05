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

class FileTileReader(
  val attributeStore: AttributeStore,
  catalogPath: String
) extends TileReader[LayerId] {

  def read[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](layerId: LayerId): Reader[K, V] = new Reader[K, V] {

    val header = attributeStore.readHeader[FileLayerHeader](layerId)
    val keyIndex = attributeStore.readKeyIndex[K](layerId)
    val writerSchema = attributeStore.readSchema(layerId)

    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = KeyPathGenerator(catalogPath, header.path, keyIndex, maxWidth)

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
  def apply(catalogPath: String): FileTileReader =
    new FileTileReader(new FileAttributeStore(catalogPath), catalogPath)

  def apply(attributeStore: FileAttributeStore): FileTileReader =
    new FileTileReader(attributeStore, attributeStore.catalogPath)
}
