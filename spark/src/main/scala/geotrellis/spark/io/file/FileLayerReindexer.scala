package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._

import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.ClassTag

object FileLayerReindexer {
  def apply[
    K: AvroRecordCodec: JsonFormat: Boundable: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](
    attributeStore: FileAttributeStore,
    keyIndexMethod: KeyIndexMethod[K]
  )(implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val layerReader = FileLayerReader[K, V, M](attributeStore)
    val layerDeleter = FileLayerDeleter[K, V, M](attributeStore)
    val layerWriter = FileLayerWriter[K, V, M](attributeStore, keyIndexMethod)

    val layerCopier = new SparkLayerCopier[FileLayerHeader, K, V, M](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    )

    val layerMover = GenericLayerMover(layerCopier, layerDeleter)

    GenericLayerReindexer(layerDeleter, layerCopier, layerMover)
  }

  def apply[
    K: AvroRecordCodec: JsonFormat: Boundable: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](catalogPath: String, keyIndexMethod: KeyIndexMethod[K])(implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val attributeStore = FileAttributeStore(catalogPath)
    apply[K, V, M](attributeStore, keyIndexMethod)
  }
}
