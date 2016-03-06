package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.spark.rdd.RDD
import com.typesafe.scalalogging.slf4j._
import spray.json._

import scala.reflect._

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
class FileLayerWriter[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
    val attributeStore: AttributeStore[JsonFormat],
    rddWriter: FileRDDWriter[K, V],
    keyIndexMethod: KeyIndexMethod[K],
    catalogPath: String,
    clobber: Boolean = true,
    oneToOne: Boolean = false
) extends Writer[LayerId, RDD[(K, V)] with Metadata[M]] with LazyLogging {

  def write(layerId: LayerId, rdd: RDD[(K, V)] with Metadata[M]) = {
    val catalogPathFile = new File(catalogPath)

    require(!attributeStore.layerExists(layerId) || clobber, s"$layerId already exists")

    val path = LayerPath(layerId)
    val metadata = rdd.metadata
    val header =
      FileLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        path = path
      )

    val keyBounds = implicitly[Boundable[K]].collectBounds(rdd)
      .getOrElse(throw new LayerWriteError(layerId, "empty rdd write"))
    val keyIndex = keyIndexMethod.createIndex(keyBounds)
    val maxWidth = Index.digits(keyIndex.toIndex(keyBounds.maxKey))
    val keyPath = KeyPathGenerator(catalogPath, path, keyIndex, maxWidth)
    val layerPath = new File(catalogPath, path).getAbsolutePath

    try {
      attributeStore.writeLayerAttributes(layerId, header, metadata, keyBounds, keyIndex, rddWriter.schema)

      logger.info(s"Saving RDD ${layerId.name} to ${catalogPath}")
      rddWriter.write(rdd, layerPath, keyPath, oneToOne = oneToOne)
    } catch {
      case e: Exception => throw new LayerWriteError(layerId).initCause(e)
    }
  }
}

object FileLayerWriter {
  case class Options(
    clobber: Boolean = true,
    oneToOne: Boolean = false
  )

  object Options {
    def DEFAULT = Options()
  }

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](attributeStore: FileAttributeStore, keyIndexMethod: KeyIndexMethod[K], options: Options): FileLayerWriter[K, V, M] =
    new FileLayerWriter[K, V, M](
      attributeStore,
      new FileRDDWriter[K, V],
      keyIndexMethod,
      attributeStore.catalogPath,
      options.clobber,
      options.oneToOne
    )

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](attributeStore: FileAttributeStore, keyIndexMethod: KeyIndexMethod[K]): FileLayerWriter[K, V, M] =
    apply[K, V, M](attributeStore, keyIndexMethod, Options.DEFAULT)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](catalogPath: String, keyIndexMethod: KeyIndexMethod[K], options: Options): FileLayerWriter[K, V, M] =
    apply[K, V, M](FileAttributeStore(catalogPath), keyIndexMethod, options)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](catalogPath: String, keyIndexMethod: KeyIndexMethod[K]): FileLayerWriter[K, V, M] =
    apply[K, V, M](catalogPath, keyIndexMethod, Options.DEFAULT)

  def spatial(
    catalogPath: String,
    keyIndexMethod: KeyIndexMethod[SpatialKey],
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): FileLayerWriter[SpatialKey, Tile, RasterMetaData] =
    apply[SpatialKey, Tile, RasterMetaData](catalogPath, keyIndexMethod, Options(clobber, oneToOne))

  def spatialMultiBand(
    catalogPath: String,
    keyIndexMethod: KeyIndexMethod[SpatialKey],
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): FileLayerWriter[SpatialKey, MultiBandTile, RasterMetaData] =
    apply[SpatialKey, MultiBandTile, RasterMetaData](catalogPath, keyIndexMethod, Options(clobber, oneToOne))

  def spaceTime(
    catalogPath: String,
    keyIndexMethod: KeyIndexMethod[SpaceTimeKey],
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): FileLayerWriter[SpaceTimeKey, Tile, RasterMetaData] =
    apply[SpaceTimeKey, Tile, RasterMetaData](catalogPath, keyIndexMethod, Options(clobber, oneToOne))

  def spaceTimeMultiBand(
    catalogPath: String,
    keyIndexMethod: KeyIndexMethod[SpaceTimeKey],
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): FileLayerWriter[SpaceTimeKey, MultiBandTile, RasterMetaData] =
    apply[SpaceTimeKey, MultiBandTile, RasterMetaData](catalogPath, keyIndexMethod, Options(clobber, oneToOne))
}
