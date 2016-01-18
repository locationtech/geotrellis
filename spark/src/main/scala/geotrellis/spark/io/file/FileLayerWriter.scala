package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._

import geotrellis.raster._
import geotrellis.raster.io.Filesystem

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
  * @param clobber           flag to overwrite raster if already present on File
  * @param attributeStore    AttributeStore to be used for storing raster metadata
  */
class FileLayerWriter[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
    val attributeStore: AttributeStore[JsonFormat],
    rddWriter: FileRDDWriter[K, V],
    catalogPath: String,
    clobber: Boolean = true,
    oneToOne: Boolean = false
) extends Writer[LayerId, K, RDD[(K, V)] with Metadata[M]] with LazyLogging {

  def write[I <: KeyIndex[K]: JsonFormat](layerId: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: I): Unit = {
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

    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
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

  def write(id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndexMethod: KeyIndexMethod[K]): Unit = {
    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
    write(id, rdd, keyIndexMethod.createIndex(keyBounds))
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
  ](attributeStore: FileAttributeStore, options: Options): FileLayerWriter[K, V, M] =
    new FileLayerWriter[K, V, M](
      attributeStore,
      new FileRDDWriter[K, V],
      attributeStore.catalogPath,
      options.clobber,
      options.oneToOne
    )

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](attributeStore: FileAttributeStore): FileLayerWriter[K, V, M] =
    apply[K, V, M](attributeStore, Options.DEFAULT)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](catalogPath: String, options: Options): FileLayerWriter[K, V, M] =
    apply[K, V, M](FileAttributeStore(catalogPath), options)

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](catalogPath: String): FileLayerWriter[K, V, M] =
    apply[K, V, M](catalogPath, Options.DEFAULT)

  def spatial(
    catalogPath: String,
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): FileLayerWriter[SpatialKey, Tile, RasterMetaData] =
    apply[SpatialKey, Tile, RasterMetaData](catalogPath, Options(clobber, oneToOne))

  def spatialMultiBand(
    catalogPath: String,
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): FileLayerWriter[SpatialKey, MultiBandTile, RasterMetaData] =
    apply[SpatialKey, MultiBandTile, RasterMetaData](catalogPath, Options(clobber, oneToOne))

  def spaceTime(
    catalogPath: String,
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): FileLayerWriter[SpaceTimeKey, Tile, RasterMetaData] =
    apply[SpaceTimeKey, Tile, RasterMetaData](catalogPath, Options(clobber, oneToOne))

  def spaceTimeMultiBand(
    catalogPath: String,
    clobber: Boolean = true,
    oneToOne: Boolean = false
  ): FileLayerWriter[SpaceTimeKey, MultiBandTile, RasterMetaData] =
    apply[SpaceTimeKey, MultiBandTile, RasterMetaData](catalogPath, Options(clobber, oneToOne))
}
