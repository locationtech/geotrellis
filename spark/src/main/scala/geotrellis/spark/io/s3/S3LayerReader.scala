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
import org.apache.spark.rdd.RDD
import spray.json.{JsObject, JsonFormat}
import AttributeStore.Fields

import scala.reflect.ClassTag


/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @param getCache        Optional cache function to be used when reading S3 objects.
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V              Type of RDD Value (ex: Tile or MultiBandTile )
 * @tparam Container      Type of RDD Container that composes RDD and it's metadata (ex: RasterRDD or MultiBandRasterRDD)
 */
class S3LayerReader[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, Container <: RDD[(K,V)]](
    val attributeStore: AttributeStore[JsonFormat],
    rddReader: S3RDDReader[K, V],
    getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None)
  (implicit sc: SparkContext, val cons: ContainerConstructor[K, V, M, Container])
  extends FilteringLayerReader[LayerId, K, M, Container] with LazyLogging {

  val defaultNumPartitions = sc.defaultParallelism

  def read(id: LayerId, rasterQuery: RDDQuery[K, M], numPartitions: Int): Container = {
    if(!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val (header, metadata, keyBounds, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[S3LayerHeader, M, KeyBounds[K], KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val bucket = header.bucket
    val prefix = header.key

    val queryKeyBounds = rasterQuery(metadata, keyBounds)
    val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
    val keyPath = (index: Long) => makePath(prefix, encodeIndex(index, maxWidth))
    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
    val cache = getCache.map(f => f(id))
    val rdd = rddReader.read(bucket, keyPath, queryKeyBounds, decompose, Some(writerSchema), cache, numPartitions)

    cons.makeContainer(rdd, keyBounds, metadata)
  }
}

object S3LayerReader {
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,  V: AvroRecordCodec: ClassTag, M: JsonFormat, Container <: RDD[(K, V)]](
      bucket: String,
      prefix: String,
      getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None)
    (implicit sc: SparkContext, cons: ContainerConstructor[K, V, M, Container]): S3LayerReader[K, V, M, Container] =
    new S3LayerReader[K, V, M, Container](
      new S3AttributeStore(bucket, prefix),
      new S3RDDReader[K, V],
      getCache)
}
