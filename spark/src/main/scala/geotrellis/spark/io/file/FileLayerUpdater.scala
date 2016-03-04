package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._

import com.typesafe.scalalogging.slf4j._
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._

import java.io.File
import scala.reflect._

class FileLayerUpdater(
  catalogPath: String,
  attributeStore: AttributeStore[JsonFormat],
  layerReader: FileLayerReader
) extends LayerUpdater[LayerId] with LazyLogging {

  protected def _update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyBounds: KeyBounds[K]) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val (header, _, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[FileLayerHeader, M, KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    val path = header.path

    if (!(keyIndex.keyBounds contains keyBounds))
      throw new LayerOutOfKeyBoundsError(id, keyIndex.keyBounds)

    val maxWidth = Index.digits(keyIndex.toIndex(keyIndex.keyBounds.maxKey))
    val keyPath = KeyPathGenerator(catalogPath, path, keyIndex, maxWidth)
    val layerPath = new File(catalogPath, path).getAbsolutePath

    logger.info(s"Saving updated RDD for layer ${id} to $path")
    if(schemaHasChanged[K, V](writerSchema)) {
      logger.warn(s"RDD schema has changed, this requires rewriting the entire layer.")
      val entireLayer = layerReader.read[K, V, M](id)
      val updated: RDD[(K, V)] with Metadata[M] =
        entireLayer.withContext { allTiles =>
          allTiles
            .leftOuterJoin(rdd)
            .mapValues { case (layerTile, updateTile) =>
              updateTile.getOrElse(layerTile)
            }
        }

      FileRDDWriter.write[K, V](updated, layerPath, keyPath)
    } else {
      FileRDDWriter.write[K, V](rdd, layerPath, keyPath)
    }
  }
}

object FileLayerUpdater {
  def apply(catalogPath: String)(implicit sc: SparkContext): FileLayerUpdater =
    new FileLayerUpdater(
      catalogPath,
      FileAttributeStore(catalogPath),
      FileLayerReader(catalogPath)
    )
}
