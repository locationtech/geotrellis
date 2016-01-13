package geotrellis.spark.io.hadoop

import geotrellis.spark.{KeyBounds, LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import org.apache.avro.Schema
import org.apache.spark.SparkContext

import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._

import scala.reflect.ClassTag
import org.apache.hadoop.fs.Path

object HadoopLayerReindexer {
  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat](rootPath: Path)(implicit sc: SparkContext, format: HadoopFormat[K, V]): LayerReindexer[LayerId, K] = {
    val attributeStore = HadoopAttributeStore(new Path(rootPath, "attributes"))
    val layerReader    = HadoopLayerReader[K, V, M](rootPath)
    val layerDeleter   = HadoopLayerDeleter(rootPath)
    val layerMover     = HadoopLayerMover[K, V, M](rootPath)
    val layerWriter    = HadoopLayerWriter[K, V, M](rootPath)

    val layerCopier = new SparkLayerCopier[HadoopLayerHeader, K, V, M](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: HadoopLayerHeader): HadoopLayerHeader =
        header.copy(path = new Path(rootPath, s"${id.name}/${id.zoom}"))
    }

    GenericLayerReindexer(layerDeleter, layerCopier, layerMover)
  }
}
