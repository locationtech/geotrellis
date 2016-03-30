package geotrellis.spark.io.s3

import geotrellis.raster.{MultibandTile, Tile}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.util.cache._
import geotrellis.util._

import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V              Type of RDD Value (ex: Tile or MultibandTile )
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 */
class S3LayerReader(val attributeStore: AttributeStore)(implicit sc: SparkContext)
  extends FilteringLayerReader[LayerId] with LazyLogging {

  val defaultNumPartitions = sc.defaultParallelism

  def rddReader: S3RDDReader = S3RDDReader

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, rasterQuery: LayerQuery[K, M], numPartitions: Int, filterIndexOnly: Boolean) = {
    if(!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[S3LayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val bucket = header.bucket
    val prefix = header.key

    val queryKeyBounds = rasterQuery(metadata)
    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = (index: Long) => makePath(prefix, Index.encode(index, maxWidth))
    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
    val rdd = rddReader.read[K, V](bucket, keyPath, queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema), Some(numPartitions))

    new ContextRDD(rdd, metadata)
  }
}

object S3LayerReader {
  def apply(attributeStore: AttributeStore)(implicit sc: SparkContext): S3LayerReader =
    new S3LayerReader(attributeStore)

  def apply(bucket: String, prefix: String)(implicit sc: SparkContext): S3LayerReader =
    apply(new S3AttributeStore(bucket, prefix))
}
