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
class FileLayerReader[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
  val attributeStore: AttributeStore[JsonFormat],
  catalogPath: String,
  rddReader: FileRDDReader[K, V],
  getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None
)(implicit sc: SparkContext) extends FilteringLayerReader[LayerId, K, M, RDD[(K, V)] with Metadata[M]] with LazyLogging {

  val defaultNumPartitions = sc.defaultParallelism

  def read(id: LayerId, rasterQuery: RDDQuery[K, M], numPartitions: Int) = {
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
    val rdd = rddReader.read(keyPath, queryKeyBounds, decompose, Some(writerSchema), cache, numPartitions)

    new ContextRDD(rdd, metadata)
  }
}

object FileLayerReader {
  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](
    attributeStore: AttributeStore[JsonFormat],
    catalogPath: String,
    getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None
  )(implicit sc: SparkContext): FileLayerReader[K, V, M] =
    new FileLayerReader[K, V, M](
      attributeStore,
      catalogPath,
      new FileRDDReader[K, V],
      getCache
    )

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](attributeStore: AttributeStore[JsonFormat], catalogPath: String)(implicit sc: SparkContext): FileLayerReader[K, V, M] =
    apply[K, V, M](attributeStore, catalogPath, None)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](catalogPath: String, getCache: Option[LayerId => Cache[Long, Array[Byte]]])(implicit sc: SparkContext): FileLayerReader[K, V, M] =
    apply[K, V, M](new FileAttributeStore(catalogPath), catalogPath, getCache)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](catalogPath: String)(implicit sc: SparkContext): FileLayerReader[K, V, M] =
    apply[K, V, M](catalogPath, None)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](attributeStore: FileAttributeStore)(implicit sc: SparkContext): FileLayerReader[K, V, M] =
    apply[K, V, M](attributeStore, attributeStore.catalogPath, None)

  def spatial(catalogPath: String)(implicit sc: SparkContext): FileLayerReader[SpatialKey, Tile, RasterMetaData] =
    apply[SpatialKey, Tile, RasterMetaData](catalogPath)

  def spatialMultiBand(catalogPath: String)(implicit sc: SparkContext): FileLayerReader[SpatialKey, MultiBandTile, RasterMetaData] =
    apply[SpatialKey, MultiBandTile, RasterMetaData](catalogPath)

  def spaceTime(catalogPath: String)(implicit sc: SparkContext): FileLayerReader[SpaceTimeKey, Tile, RasterMetaData] =
    apply[SpaceTimeKey, Tile, RasterMetaData](catalogPath)


  def spaceTimeMultiBand(catalogPath: String)(implicit sc: SparkContext): FileLayerReader[SpaceTimeKey, MultiBandTile, RasterMetaData] =
    apply[SpaceTimeKey, MultiBandTile, RasterMetaData](catalogPath)
}
