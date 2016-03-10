package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.raster.{MultibandTile, Tile}

import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from a filesystem.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @tparam K              Type of RDD Key (ex: GridKey)
 * @tparam V              Type of RDD Value (ex: Tile or MultibandTile )
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 */
class FileLayerReader(
  val attributeStore: AttributeStore[JsonFormat],
  catalogPath: String
)(implicit sc: SparkContext) extends FilteringLayerReader[LayerId] with LazyLogging {

  val defaultNumPartitions = sc.defaultParallelism

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rasterQuery: RDDQuery[K, M], numPartitions: Int, filterIndexOnly: Boolean) = {
    if(!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val (header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[FileLayerHeader, M, KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val layerPath = header.path

    val queryKeyBounds = rasterQuery(metadata)
    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = KeyPathGenerator(catalogPath, layerPath, maxWidth)
    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
    val rdd = FileRDDReader.read[K, V](keyPath, queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema), Some(numPartitions))

    new ContextRDD(rdd, metadata)
  }
}

object FileLayerReader {
  def apply(attributeStore: AttributeStore[JsonFormat], catalogPath: String)(implicit sc: SparkContext): FileLayerReader =
    new FileLayerReader(attributeStore, catalogPath)

  def apply(catalogPath: String)(implicit sc: SparkContext): FileLayerReader =
    apply(new FileAttributeStore(catalogPath), catalogPath)

  def apply(attributeStore: FileAttributeStore)(implicit sc: SparkContext): FileLayerReader =
    apply(attributeStore, attributeStore.catalogPath)
}
