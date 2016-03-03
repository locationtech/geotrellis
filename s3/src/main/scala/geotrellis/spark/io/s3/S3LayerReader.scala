package geotrellis.spark.io.s3

import geotrellis.raster.{MultiBandTile, Tile}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import org.apache.avro.Schema
import geotrellis.spark.utils.cache._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.{JsObject, JsonFormat}
import AttributeStore.Fields
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.reflect.ClassTag


/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @param getCache        Optional cache function to be used when reading S3 objects.
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V              Type of RDD Value (ex: Tile or MultiBandTile )
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 */
class S3LayerReader(
  val attributeStore: AttributeStore[JsonFormat],
  getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None
)(implicit sc: SparkContext)
  extends FilteringLayerReader[LayerId] with LazyLogging {

  val defaultNumPartitions = sc.defaultParallelism

  def rddReader: S3RDDReader = S3RDDReader

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rasterQuery: RDDQuery[K, M], numPartitions: Int) = {
    if(!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val (header, metadata, keyBounds, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[S3LayerHeader, M, KeyBounds[K], KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val bucket = header.bucket
    val prefix = header.key

    val queryKeyBounds = rasterQuery(metadata, keyBounds)
    val maxWidth = Index.digits(keyIndex.toIndex(keyBounds.maxKey))
    val keyPath = (index: Long) => makePath(prefix, Index.encode(index, maxWidth))
    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
    val cache = getCache.map(f => f(id))
    val rdd = rddReader.read[K, V](bucket, keyPath, queryKeyBounds, decompose, Some(writerSchema), cache, Some(numPartitions))

    new ContextRDD(rdd, metadata)
  }
}

object S3LayerReader {
  def apply(
    attributeStore: AttributeStore[JsonFormat],
    getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None
  )(implicit sc: SparkContext): S3LayerReader =
    new S3LayerReader(attributeStore, getCache)

  def apply(attributeStore: AttributeStore[JsonFormat])(implicit sc: SparkContext): S3LayerReader =
    apply(attributeStore, None)

  def apply(bucket: String, prefix: String, getCache: Option[LayerId => Cache[Long, Array[Byte]]])(implicit sc: SparkContext): S3LayerReader =
    apply(new S3AttributeStore(bucket, prefix), getCache)

  def apply(bucket: String, prefix: String)(implicit sc: SparkContext): S3LayerReader =
    apply(bucket, prefix, None)
}
