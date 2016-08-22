package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._

import geotrellis.util._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import spray.json._

import scala.reflect.ClassTag

/**
  * Handles reading raster RDDs and their metadata from S3.
  *
  * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
  */
class HadoopLayerCollectionReader(
  val attributeStore: AttributeStore,
  conf: Configuration,
  maxOpenFiles: Int = 16
)
  extends CollectionLayerReader[LayerId] with LazyLogging {

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, rasterQuery: LayerQuery[K, M], indexFilterOnly: Boolean): Seq[(K, V)] with Metadata[M] = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val layerPath = header.path
    val keyBounds = metadata.getComponent[Bounds[K]].getOrElse(throw new LayerEmptyBoundsError(id))
    val queryKeyBounds = rasterQuery(metadata)

    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)

    val seq = HadoopCollectionReader(maxOpenFiles).read[K, V](layerPath, conf, queryKeyBounds, decompose, indexFilterOnly, Some(writerSchema))

    new ContextCollection[K, V, M](seq, metadata)
  }
}

object HadoopLayerCollectionReader {
  def apply(attributeStore: HadoopAttributeStore): HadoopLayerCollectionReader =
    new HadoopLayerCollectionReader(attributeStore, attributeStore.hadoopConfiguration)

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopLayerCollectionReader =
    apply(HadoopAttributeStore(rootPath))

  def apply(rootPath: Path, conf: Configuration): HadoopLayerCollectionReader =
    apply(HadoopAttributeStore(rootPath, conf))
}
