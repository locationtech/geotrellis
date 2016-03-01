package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.raster._
import geotrellis.util.Filesystem

import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect._
import com.typesafe.scalalogging.slf4j._
import AttributeStore.Fields

import java.io.File


/**
  * Handles writing Raster RDDs and their metadata to a filesystem.
  *
  * @tparam K                Type of RDD Key (ex: SpatialKey)
  * @tparam V                Type of RDD Value (ex: Tile or MultiBandTile )
  * @tparam M                Type of Metadata associated with the RDD[(K,V)]
  *
  * @param catalogPath  The root directory of this catalog.
  * @param keyPrefix         File prefix to write the raster to
  * @param keyIndexMethod    Method used to convert RDD keys to SFC indexes
  * @param clobber           flag to overwrite raster if already present on File
  * @param attributeStore    AttributeStore to be used for storing raster metadata
  */
class FileLayerWriter(
    val attributeStore: AttributeStore[JsonFormat],
    catalogPath: String,
    clobber: Boolean = true
) extends LayerWriter[LayerId] with LazyLogging {

  def write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](layerId: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K], keyBounds: KeyBounds[K]): Unit = {
    val catalogPathFile = new File(catalogPath)

    val codec  = KeyValueRecordCodec[K, V]
    val schema = codec.schema

    require(!attributeStore.layerExists(layerId) || clobber, s"$layerId already exists")

    val path = LayerPath(layerId)
    val metadata = rdd.metadata
    val header =
      FileLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        path = path
      )

    val maxWidth = Index.digits(keyIndex.toIndex(keyBounds.maxKey))
    val keyPath = KeyPathGenerator(catalogPath, path, keyIndex, maxWidth)
    val layerPath = new File(catalogPath, path).getAbsolutePath

    try {
      attributeStore.writeLayerAttributes(layerId, header, metadata, keyBounds, keyIndex, schema)

      logger.info(s"Saving RDD ${layerId.name} to ${catalogPath}")
      FileRDDWriter.write(rdd, layerPath, keyPath)
    } catch {
      case e: Exception => throw new LayerWriteError(layerId).initCause(e)
    }
  }
}

object FileLayerWriter {
  def apply(attributeStore: FileAttributeStore, clobber: Boolean): FileLayerWriter =
    new FileLayerWriter(
      attributeStore,
      attributeStore.catalogPath,
      clobber
    )

  def apply(attributeStore: FileAttributeStore): FileLayerWriter =
    apply(attributeStore, true)

  def apply(catalogPath: String, clobber: Boolean): FileLayerWriter =
    apply(FileAttributeStore(catalogPath), clobber)

  def apply(catalogPath: String): FileLayerWriter =
    apply(catalogPath, true)

}
