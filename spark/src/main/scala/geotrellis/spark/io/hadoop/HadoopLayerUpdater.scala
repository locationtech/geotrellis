package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._

import com.typesafe.scalalogging.slf4j._
import org.apache.avro.Schema
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect._

class HadoopLayerUpdater(
  attributeStore: AttributeStore[JsonFormat],
  layerReader: HadoopLayerReader
) extends LayerUpdater[LayerId] with LazyLogging {

  protected def _update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyBounds: KeyBounds[K]) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val (header, _, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, M, KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    val path = header.path

    if (!(keyIndex.keyBounds contains keyBounds))
      throw new LayerOutOfKeyBoundsError(id, keyIndex.keyBounds)

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

      HadoopRDDWriter.write[K, V](updated, path, keyIndex)
    } else {
      HadoopRDDWriter.write[K, V](rdd, path, keyIndex)
    }
  }
}

object HadoopLayerUpdater {
  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopLayerUpdater =
    new HadoopLayerUpdater(
      HadoopAttributeStore(rootPath),
      HadoopLayerReader(rootPath)
    )
}
