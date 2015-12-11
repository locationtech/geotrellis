package geotrellis.spark.io.accumulo

import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import scala.reflect.ClassTag

object AccumuloLayerMover {
  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_]](
   instance: AccumuloInstance,
   layerReader: AccumuloLayerReader[K, V, Container[K]],
   layerWriter: AccumuloLayerWriter[K, V, Container[K]])
  (implicit sc: SparkContext,
          cons: ContainerConstructor[K, V, Container[K]],
   containerEv: Container[K] => Container[K] with RDD[(K, V)]): LayerMover[LayerId] = {
    val attributeStore = AccumuloAttributeStore(instance.connector)
    new LayerMover(
      attributeStore,
      AccumuloLayerCopier[K, V, Container](
        attributeStore = attributeStore,
        layerReader    = layerReader,
        layerWriter    = layerWriter
      ),
      AccumuloLayerDeleter(AccumuloAttributeStore(instance.connector), instance.connector)
    )
  }

  def apply(instance: AccumuloInstance,
            layerCopier: LayerCopier[LayerId],
            layerDeleter: LayerDeleter[LayerId]): LayerMover[LayerId] = {
    val attributeStore = AccumuloAttributeStore(instance.connector)
    new LayerMover(attributeStore, layerCopier, layerDeleter)
  }

  def apply(attributeStore: AttributeStore[JsonFormat],
            layerCopier: LayerCopier[LayerId],
            layerDeleter: LayerDeleter[LayerId]): LayerMover[LayerId] = {
    new LayerMover(attributeStore, layerCopier, layerDeleter)
  }
}
