package geotrellis.spark.io.file

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.util._
import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from a filesystem.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V              Type of RDD Value (ex: Tile or MultibandTile )
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 */
class FileLayerCollectionReader(
  val attributeStore: AttributeStore,
  catalogPath: String
) extends CollectionLayerReader[LayerId] with LazyLogging {

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, rasterQuery: LayerQuery[K, M], filterIndexOnly: Boolean) = {
    if(!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[FileLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val layerPath = header.path

    val queryKeyBounds = rasterQuery(metadata)
    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = KeyPathGenerator(catalogPath, layerPath, maxWidth)
    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
    val seq = FileCollectionReader.read[K, V](keyPath, queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema))

    new ContextCollection(seq, metadata)
  }
}

object FileLayerCollectionReader {
  def apply(attributeStore: AttributeStore, catalogPath: String): FileLayerCollectionReader =
    new FileLayerCollectionReader(attributeStore, catalogPath)

  def apply(catalogPath: String): FileLayerCollectionReader =
    apply(new FileAttributeStore(catalogPath), catalogPath)

  def apply(attributeStore: FileAttributeStore): FileLayerCollectionReader =
    apply(attributeStore, attributeStore.catalogPath)
}


