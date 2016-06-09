package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import geotrellis.spark.merge._
import geotrellis.spark.util._
import geotrellis.util._

import com.typesafe.scalalogging.slf4j._
import org.apache.avro.Schema
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect._

class HadoopLayerUpdater(
  attributeStore: AttributeStore,
  layerReader: HadoopLayerReader,
  layerWriter: HadoopLayerWriter,
  layerDeleter: HadoopLayerDeleter,
  layerCopier: HadoopLayerCopier
) extends LayerUpdater[LayerId] with LazyLogging {

  protected def _update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyBounds: KeyBounds[K], mergeFunc: (V, V) => V) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader,M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    if (!(keyIndex.keyBounds contains keyBounds))
      throw new LayerOutOfKeyBoundsError(id, keyIndex.keyBounds)

    logger.warn(s"MapFiles cannot be updated, so this requires rewriting the entire layer.")

    val entireLayer = layerReader.read[K, V, M](id)

    val updatedMetadata: M =
      metadata.merge(rdd.metadata)

    val updatedRdd: RDD[(K, V)] =
      entireLayer
        .fullOuterJoin(rdd)
        .flatMapValues {
          case (Some(layerTile), Some(updateTile)) => Some(mergeFunc(layerTile, updateTile))
          case (Some(layerTile), _) => Some(layerTile)
          case (_, Some(updateTile)) => Some(updateTile)
          case _ => None
        }

    val updated = ContextRDD(updatedRdd, updatedMetadata)

    val tmpId = id.createTemporaryId
    logger.info(s"Saving updated RDD to temporary id $tmpId")
    layerWriter.write(tmpId, updated, keyIndex)
    logger.info(s"Deleting layer $id")
    layerDeleter.delete(id)
    logger.info(s"Copying in $tmpId to $id")
    layerCopier.copy[K, V, M](tmpId, id)
    logger.info(s"Deleting temporary layer at $tmpId")
    layerDeleter.delete(tmpId)
  }
}

object HadoopLayerUpdater {
  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopLayerUpdater =
    new HadoopLayerUpdater(
      HadoopAttributeStore(rootPath),
      HadoopLayerReader(rootPath),
      HadoopLayerWriter(rootPath),
      HadoopLayerDeleter(rootPath),
      HadoopLayerCopier(rootPath)
    )
}
