package geotrellis.spark.io.hadoop

import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import spray.json.JsonFormat
import scala.reflect.ClassTag

class HadoopLayerReindexer[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, Container](
   rootPath: String, keyIndexMethod: KeyIndexMethod[K])
  (implicit sc: SparkContext,
        format: HadoopFormat[K, V],
      val cons: ContainerConstructor[K, V, Container]) extends LayerReindexer[LayerId, K, Container] {

  lazy val attributeStore = HadoopAttributeStore(rootPath)
  lazy val layerReader    = new HadoopLayerReader[K, V, Container](attributeStore, new HadoopRDDReader[K, V](HadoopCatalogConfig.DEFAULT))
  lazy val layerDeleter   = HadoopLayerDeleter(rootPath)
  lazy val layerMover     = new HadoopLayerMover[K, V, Container](attributeStore = attributeStore, rootPath = rootPath)
  lazy val layerCopier    = new HadoopLayerCopier[K, V, Container](
    attributeStore = attributeStore,
    rootPath       = rootPath
  )

  def tmpId(id: LayerId): LayerId = id.copy(name = s"${id.name}-tmp")
}
