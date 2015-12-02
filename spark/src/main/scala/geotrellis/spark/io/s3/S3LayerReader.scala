package geotrellis.spark.io.s3

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndex
import org.apache.avro.Schema
import geotrellis.spark.utils.cache._
import org.apache.spark.SparkContext
import spray.json.{JsObject, JsonFormat}
import spray.json.DefaultJsonProtocol._
import AttributeStore.Fields

import scala.reflect.ClassTag


/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @param getCache        Optional cache function to be used when reading S3 objects.
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V       Type of RDD Value (ex: Tile or MultiBandTile )
 * @tparam Container      Type of RDD Container that composes RDD and it's metadata (ex: RasterRDD or MultiBandRasterRDD)
 */
class S3LayerReader[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container](
    val attributeStore: AttributeStore[JsonFormat],
    rddReader: S3RDDReader[K, V],
    getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None)
  (implicit sc: SparkContext, val cons: ContainerConstructor[K, V, Container])
  extends FilteringLayerReader[LayerId, K, Container] with LazyLogging {

  type MetaDataType  = cons.MetaDataType

  val defaultNumPartitions = sc.defaultParallelism

  def read(id: LayerId, rasterQuery: RDDQuery[K, MetaDataType], numPartitions: Int): Container = {
    try {
      if(!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
      implicit val mdFormat = cons.metaDataFormat
      val (header, metadata, keyBounds, keyIndex, writerSchema) =
        attributeStore.readLayerAttributes[S3LayerHeader, MetaDataType, KeyBounds[K], KeyIndex[K], Schema](id)
      val bucket = header.bucket
      val prefix = header.key

      val queryKeyBounds = rasterQuery(metadata, keyBounds)
      val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
      val keyPath = (index: Long) => makePath(prefix, encodeIndex(index, maxWidth))
      val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
      val cache = getCache.map(f => f(id))
      val rdd = rddReader.read(bucket, keyPath, queryKeyBounds, decompose, Some(writerSchema), cache, numPartitions)

      cons.makeContainer(rdd, keyBounds, metadata)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }
  }
}

object S3LayerReader {
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, Container[_]](
      bucket: String,
      prefix: String,
      getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None)
    (implicit sc: SparkContext, cons: ContainerConstructor[K, V, Container[K]]): S3LayerReader[K, V, Container[K]] =
    new S3LayerReader(
      new S3AttributeStore(bucket, prefix),
      new S3RDDReader[K, V],
      getCache)
}