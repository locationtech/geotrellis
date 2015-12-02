package geotrellis.spark.io.hadoop

import geotrellis.spark.Boundable
import geotrellis.spark.io.{LayerCopier, ContainerConstructor}
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import scala.reflect.ClassTag

object HadoopLayerCopier {
  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_] <: RDD[(K, V)]]
  (rootPath: Path, indexMethod: KeyIndexMethod[K])
  (implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container[K]], format: HadoopFormat[K, V]): LayerCopier[HadoopLayerHeader, K, V, Container[K]] =
    new LayerCopier[HadoopLayerHeader, K, V, Container[K]](
      attributeStore = HadoopAttributeStore(new Path(rootPath, "attributes"), new Configuration),
      layerReader = HadoopLayerReader[K, V, Container](rootPath),
      layerWriter = HadoopLayerWriter[K, V, Container](rootPath, indexMethod)
    )
}
