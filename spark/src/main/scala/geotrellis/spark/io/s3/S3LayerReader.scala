package geotrellis.spark.io.s3

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndex
import org.apache.avro.Schema
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
 * @tparam TileType       Type of RDD Value (ex: Tile or MultiBandTile )
 * @tparam Container      Type of RDD Container that composes RDD and it's metadata (ex: RasterRDD or MultiBandRasterRDD)
 */
class S3LayerReader[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, TileType: AvroRecordCodec: ClassTag, Container[_]](
    val attributeStore: S3AttributeStore,
    getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None)
  (implicit sc: SparkContext, val cons: ContainerConstructor[K, TileType, Container])
  extends FilteringRasterRDDReader[K, Container[K]] with LazyLogging {

  type MetaDataType  = cons.MetaDataType

  val getS3Client: () => S3Client = () => S3Client.default
  val defaultNumPartitions = sc.defaultParallelism

  def read(id: LayerId, rasterQuery: RDDQuery[K, MetaDataType], numPartitions: Int): Container[K] = {
    val layerMetaData  = attributeStore.cacheRead[S3LayerMetaData](id, Fields.layerMetaData)
    val metadata  = attributeStore.cacheRead[cons.MetaDataType](id, Fields.rddMetadata)(cons.metaDataFormat)
    val keyBounds = attributeStore.cacheRead[KeyBounds[K]](id, Fields.keyBounds)
    val keyIndex  = attributeStore.cacheRead[KeyIndex[K]](id, Fields.keyIndex)

    val bucket = layerMetaData.bucket
    val prefix = layerMetaData.key

    val queryKeyBounds = rasterQuery(metadata, keyBounds)

    val writerSchema: Schema = (new Schema.Parser).parse(attributeStore.cacheRead[JsObject](id, "schema").toString())
    val maxWidth = maxIndexWidth(keyIndex.toIndex(keyBounds.maxKey))
    val keyPath = (index: Long) => makePath(prefix, encodeIndex(index, maxWidth))
    val reader = new S3RDDReader[K, TileType](bucket, getS3Client)
    val cache = getCache.map(f => f(id))
    val rdd = reader.read(queryKeyBounds, keyIndex, keyPath, writerSchema, numPartitions, cache)

    cons.makeContainer(rdd, keyBounds, metadata)
  }
}

object S3LayerReader {
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, TileType: AvroRecordCodec: ClassTag, Container[_]](
      bucket: String,
      prefix: String,
      getCache: Option[LayerId => Cache[Long, Array[Byte]]] = None): S3LayerReader[K, TileType, Container] =
    new S3LayerReader[K, TileType, Container](new S3AttributeStore(bucket, prefix), getCache)
}