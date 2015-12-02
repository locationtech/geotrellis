package geotrellis.spark.io.s3

import geotrellis.spark.utils.cache.Cache
import geotrellis.spark.{LayerId, Boundable}
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.{LayerCopier, ContainerConstructor}
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import scala.reflect.ClassTag

object S3LayerCopier {
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, Container[_] <: RDD[(K, V)]]
  (bucket: String,
   prefix: String,
   keyIndexMethod: KeyIndexMethod[K],
   getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None,
   clobber: Boolean = true)
  (implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container[K]]): LayerCopier[S3LayerHeader, K, V, Container[K]] =
    new LayerCopier[S3LayerHeader, K, V, Container[K]](
      attributeStore = S3AttributeStore(bucket, prefix),
      layerReader = S3LayerReader[K, V, Container](bucket, prefix, getCache),
      layerWriter = S3LayerWriter[K, V, Container](bucket, prefix, keyIndexMethod, clobber)
    )
}
