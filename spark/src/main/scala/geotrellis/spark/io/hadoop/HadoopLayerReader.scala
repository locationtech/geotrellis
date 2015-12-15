package geotrellis.spark.io.hadoop

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io._
import geotrellis.spark.{KeyBounds, LayerId, Boundable}
import geotrellis.spark.io.json._
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect.ClassTag


/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V       Type of RDD Value (ex: Tile or MultiBandTile )
 * @tparam Container      Type of RDD Container that composes RDD and it's metadata (ex: RasterRDD or MultiBandRasterRDD)
 */
class HadoopLayerReader[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, Container <: RDD[(K, V)]](
  val attributeStore: AttributeStore[JsonFormat],
  rddReader: HadoopRDDReader[K, V])
  (implicit sc: SparkContext, cons: ContainerConstructor[K, V, M, Container])
  extends FilteringLayerReader[LayerId, K, M, Container] with LazyLogging {

  val defaultNumPartitions = sc.defaultParallelism

  def read(id: LayerId, rasterQuery: RDDQuery[K, M], numPartitions: Int): Container = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val (header, metadata, keyBounds, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, M, KeyBounds[K], KeyIndex[K], Unit](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val layerPath = header.path
    val queryKeyBounds = rasterQuery(metadata, keyBounds)

    val rdd: RDD[(K, V)] =
      if (queryKeyBounds == Seq(keyBounds)) {
        rddReader.readFully(layerPath)
      } else {
        val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)
        rddReader.readFiltered(layerPath, queryKeyBounds, decompose)
      }

    cons.makeContainer(rdd, keyBounds, metadata)
  }
}

object HadoopLayerReader {
  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    attributeStore: HadoopAttributeStore,
    rddReader: HadoopRDDReader[K, V])
  (implicit sc: SparkContext, cons: ContainerConstructor[K, V, M, C]) =
    new HadoopLayerReader[K, V, M, C](attributeStore, rddReader)

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    rootPath: Path)
  (implicit
    sc: SparkContext,
    format: HadoopFormat[K, V],
    cons: ContainerConstructor[K, V, M, C]): HadoopLayerReader[K, V, M, C] =
    apply(HadoopAttributeStore(new Path(rootPath, "attributes")), new HadoopRDDReader[K, V](HadoopCatalogConfig.DEFAULT))
}