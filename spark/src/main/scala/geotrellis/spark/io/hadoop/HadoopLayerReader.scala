package geotrellis.spark.io.hadoop

import com.typesafe.scalalogging.slf4j.LazyLogging
import geotrellis.raster.{MultiBandTile, Tile}
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io._
import geotrellis.spark._
import geotrellis.spark.io.json._
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect.ClassTag


/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 * @tparam K              Type of RDD Key (ex: SpatialKey)
 * @tparam V       Type of RDD Value (ex: Tile or MultiBandTile )
 * @tparam M              Type of Metadata associated with the RDD[(K,V)]
 * @tparam C      Type of RDD Container that composes RDD and it's metadata (ex: RasterRDD or MultiBandRasterRDD)
 */
class HadoopLayerReader[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
  val attributeStore: AttributeStore[JsonFormat],
  rddReader: HadoopRDDReader[K, V])
  (implicit sc: SparkContext, bridge: Bridge[(RDD[(K, V)], M), C])
  extends FilteringLayerReader[LayerId, K, M, C] with LazyLogging {

  val defaultNumPartitions = sc.defaultParallelism

  def read(id: LayerId, rasterQuery: RDDQuery[K, M], numPartitions: Int): C = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val (header, metadata, keyBounds, keyIndex, writerSchema) = try {
      import spray.json.DefaultJsonProtocol._
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

    bridge(rdd -> metadata)
  }
}

object HadoopLayerReader {
  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    attributeStore: HadoopAttributeStore,
    rddReader: HadoopRDDReader[K, V])
  (implicit sc: SparkContext, bridge: Bridge[(RDD[(K, V)], M), C]) =
    new HadoopLayerReader[K, V, M, C](attributeStore, rddReader)

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    rootPath: Path)
  (implicit
    sc: SparkContext,
    format: HadoopFormat[K, V],
    cons: Bridge[(RDD[(K, V)], M), C]): HadoopLayerReader[K, V, M, C] =
    apply(HadoopAttributeStore(new Path(rootPath, "attributes")), new HadoopRDDReader[K, V](HadoopCatalogConfig.DEFAULT))

  def spatial(rootPath: Path)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpatialKey, Tile)], RasterMetaData), RasterRDD[SpatialKey]]) =
    new HadoopLayerReader[SpatialKey, Tile, RasterMetaData, RasterRDD[SpatialKey]](
      HadoopAttributeStore(new Path(rootPath, "attributes")), new HadoopRDDReader[SpatialKey, Tile](HadoopCatalogConfig.DEFAULT))

  def spatialMultiBand(rootPath: Path)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpatialKey, MultiBandTile)], RasterMetaData), MultiBandRasterRDD[SpatialKey]]) =
    new HadoopLayerReader[SpatialKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpatialKey]](
      HadoopAttributeStore(new Path(rootPath, "attributes")), new HadoopRDDReader[SpatialKey, MultiBandTile](HadoopCatalogConfig.DEFAULT))

  def spaceTime(rootPath: Path)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpaceTimeKey, Tile)], RasterMetaData), RasterRDD[SpaceTimeKey]]) =
    new HadoopLayerReader[SpaceTimeKey, Tile, RasterMetaData, RasterRDD[SpaceTimeKey]](
      HadoopAttributeStore(new Path(rootPath, "attributes")), new HadoopRDDReader[SpaceTimeKey, Tile](HadoopCatalogConfig.DEFAULT))

  def spaceTimeMultiBand(rootPath: Path)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpaceTimeKey, MultiBandTile)], RasterMetaData), MultiBandRasterRDD[SpaceTimeKey]]) =
    new HadoopLayerReader[SpaceTimeKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpaceTimeKey]](
      HadoopAttributeStore(new Path(rootPath, "attributes")), new HadoopRDDReader[SpaceTimeKey, MultiBandTile](HadoopCatalogConfig.DEFAULT))
}