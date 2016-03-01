package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.raster.{MultiBandTile, Tile}

import org.apache.avro.Schema
import geotrellis.spark.utils.cache._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.{JsObject, JsonFormat}
import AttributeStore.Fields
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.reflect.ClassTag


/**
 * Handles reading raster RDDs and their metadata from a filesystem.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @param getCache        Optional cache function to be used when reading File objects.
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V              Type of RDD Value (ex: Tile or MultiBandTile )
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 */
class FileLayerReader(
  val attributeStore: AttributeStore[JsonFormat],
  catalogPath: String,
  getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None
)(implicit sc: SparkContext) extends FilteringLayerReader[LayerId] with LazyLogging {

  val defaultNumPartitions = sc.defaultParallelism

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: KeyIndexJsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: LayerId, rasterQuery: RDDQuery[K, M], numPartitions: Int) = {
    if(!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val (header, metadata, keyBounds, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[FileLayerHeader, M, KeyBounds[K], KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val layerPath = header.path

    val queryKeyBounds = rasterQuery(metadata, keyBounds)
    val maxWidth = Index.digits(keyIndex.toIndex(keyBounds.maxKey))
    val keyPath = KeyPathGenerator(catalogPath, layerPath, maxWidth)
    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
    val cache = getCache.map(f => f(id))
    val rdd = FileRDDReader.read[K, V](keyPath, queryKeyBounds, decompose, Some(writerSchema), cache, Some(numPartitions))

    new ContextRDD(rdd, metadata)
  }
}

object FileLayerReader {
  def apply(
    attributeStore: AttributeStore[JsonFormat],
    catalogPath: String,
    getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None
  )(implicit sc: SparkContext): FileLayerReader =
    new FileLayerReader(
      attributeStore,
      catalogPath,
      getCache
    )

  def apply(attributeStore: AttributeStore[JsonFormat], catalogPath: String)(implicit sc: SparkContext): FileLayerReader =
    apply(attributeStore, catalogPath, None)

  def apply(catalogPath: String, getCache: Option[LayerId => Cache[Long, Array[Byte]]])(implicit sc: SparkContext): FileLayerReader =
    apply(new FileAttributeStore(catalogPath), catalogPath, getCache)

  def apply(catalogPath: String)(implicit sc: SparkContext): FileLayerReader =
    apply(catalogPath, None)

  def apply(attributeStore: FileAttributeStore)(implicit sc: SparkContext): FileLayerReader =
    apply(attributeStore, attributeStore.catalogPath, None)
}
