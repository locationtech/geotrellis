package geotrellis.spark.io.accumulo

import geotrellis.raster.mosaic._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import scala.reflect.ClassTag

object AccumuloLayerMover {
  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
   instance: AccumuloInstance,
   layerReader: AccumuloLayerReader[K, V, M, C],
   layerWriter: AccumuloLayerWriter[K, V, M, C])
  (implicit sc: SparkContext, bridge: Bridge[(RDD[(K, V)], M), C]): LayerMover[LayerId] = {
    val attributeStore = AccumuloAttributeStore(instance.connector)
    new GenericLayerMover[LayerId](
      layerCopier = AccumuloLayerCopier[K, V, M, C](
        attributeStore = attributeStore,
        layerReader    = layerReader,
        layerWriter    = layerWriter
      ),
      layerDeleter = AccumuloLayerDeleter(AccumuloAttributeStore(instance.connector), instance.connector)
    )
  }

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: MergeView: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    instance: AccumuloInstance,
    table: String,
    indexMethod: KeyIndexMethod[K],
    strategy: AccumuloWriteStrategy = AccumuloLayerWriter.defaultAccumuloWriteStrategy)
   (implicit sc: SparkContext, bridge: Bridge[(RDD[(K, V)], M), C]): LayerMover[LayerId] =
    apply[K, V, M, C](
      instance    = instance,
      layerReader = AccumuloLayerReader[K, V, M, C](instance),
      layerWriter = AccumuloLayerWriter[K, V, M, C](instance, table, indexMethod, strategy)
    )
}
