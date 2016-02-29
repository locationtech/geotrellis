package geotrellis.spark.io.hadoop

import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io._

import org.apache.spark.SparkContext
import spray.json.JsonFormat
import org.apache.hadoop.fs.Path

import scala.reflect.ClassTag

object HadoopLayerReindexer {
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](
    rootPath: Path, keyIndexMethod: KeyIndexMethod[K])
   (implicit sc: SparkContext, format: HadoopFormat[K, V]): LayerReindexer[LayerId] = {
    val attributeStore = HadoopAttributeStore(new Path(rootPath, "attributes"))
    val layerReader    = HadoopLayerReader[K, V, M](rootPath)
    val layerDeleter   = HadoopLayerDeleter(rootPath)
    val layerMover     = HadoopLayerMover[K, V, M](rootPath)
    val layerWriter    = HadoopLayerWriter[K, V, M](rootPath, keyIndexMethod)

    val layerCopier = new SparkLayerCopier[HadoopLayerHeader, K, V, M](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    )

    GenericLayerReindexer(layerDeleter, layerCopier, layerMover)
  }
}
