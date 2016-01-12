package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._
import geotrellis.raster.io.Filesystem
import AttributeStore.Fields

import org.apache.spark.SparkContext
import spray.json.JsonFormat
import org.apache.avro.Schema

import scala.reflect.ClassTag
import java.io.File

object FileLayerReindexer {
  def custom[
    K: AvroRecordCodec: JsonFormat: Boundable: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat,
    FI <: KeyIndex[K]: JsonFormat,
    TI <: KeyIndex[K]: JsonFormat
  ](
    attributeStore: FileAttributeStore,
    keyIndexMethod: KeyIndexMethod[K, TI]
  )(implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val layerReaderFrom = FileLayerReader.custom[K, V, M, FI](attributeStore)
    val layerReaderTo = FileLayerReader.custom[K, V, M, TI](attributeStore)
    val layerDeleter = FileLayerDeleter(attributeStore)
    val layerWriter = FileLayerWriter.custom[K, V, M, TI](attributeStore, keyIndexMethod)

    val layerCopierFrom = new SparkLayerCopier[FileLayerHeader, K, V, M, TI](
      attributeStore = attributeStore,
      layerReader    = layerReaderFrom,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(layerId: LayerId, header: FileLayerHeader): FileLayerHeader =
        header.copy(path = LayerPath(layerId))
    }

    val layerCopierTo = new SparkLayerCopier[FileLayerHeader, K, V, M, TI](
      attributeStore = attributeStore,
      layerReader    = layerReaderTo,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(layerId: LayerId, header: FileLayerHeader): FileLayerHeader =
        header.copy(path = LayerPath(layerId))
    }

    val layerMover = GenericLayerMover(layerCopierTo, layerDeleter)

    GenericLayerReindexer(layerDeleter, layerCopierFrom, layerMover)
  }

  def custom[
    K: AvroRecordCodec: JsonFormat: Boundable: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat,
    FI <: KeyIndex[K]: JsonFormat,
    TI <: KeyIndex[K]: JsonFormat
  ](catalogPath: String, keyIndexMethod: KeyIndexMethod[K, TI])(implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val attributeStore = FileAttributeStore(catalogPath)
    custom[K, V, M, FI, TI](attributeStore, keyIndexMethod)
  }

  def apply[
    K: AvroRecordCodec: JsonFormat: Boundable: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](
     attributeStore: FileAttributeStore,
     keyIndexMethod: KeyIndexMethod[K, KeyIndex[K]]
   )(implicit sc: SparkContext): LayerReindexer[LayerId] =
    custom[K, V, M, KeyIndex[K], KeyIndex[K]](attributeStore, keyIndexMethod)

  def apply[
    K: AvroRecordCodec: JsonFormat: Boundable: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](catalogPath: String, keyIndexMethod: KeyIndexMethod[K, KeyIndex[K]])(implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val attributeStore = FileAttributeStore(catalogPath)
    custom[K, V, M, KeyIndex[K], KeyIndex[K]](attributeStore, keyIndexMethod)
  }
}
