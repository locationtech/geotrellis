package geotrellis.spark.io.hadoop

import geotrellis.raster.{MultiBandTile, Tile}
import geotrellis.spark.io.json._
import geotrellis.spark._
import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import geotrellis.spark.io._
import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect._

class HadoopLayerWriter[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
  rootPath: Path,
  val attributeStore: AttributeStore[JsonFormat],
  rddWriter: HadoopRDDWriter[K, V],
  keyIndexMethod: KeyIndexMethod[K])
(implicit bridge: Bridge[(RDD[(K, V)], M), C])
  extends Writer[LayerId, C] {

  def write(id: LayerId, rdd: C): Unit = {
    implicit val sc = rdd.sparkContext

    val layerPath = new Path(rootPath,  s"${id.name}/${id.zoom}")

    val header =
      HadoopLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        path = layerPath
      )
    val (_, metaData) = bridge.unapply(rdd)
    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
    val keyIndex = keyIndexMethod.createIndex(keyBounds)

    try {
      attributeStore.writeLayerAttributes(id, header, metaData, keyBounds, keyIndex, Option.empty[Schema])
      // TODO: Writers need to handle Schema changes

      rddWriter.write(rdd, layerPath, keyIndex)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object HadoopLayerWriter {
  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    rootPath: Path,
    attributeStore: HadoopAttributeStore,
    rddWriter: HadoopRDDWriter[K, V],
    indexMethod: KeyIndexMethod[K])
  (implicit bridge: Bridge[(RDD[(K, V)], M), C]): HadoopLayerWriter[K, V, M, C] =
    new HadoopLayerWriter[K, V, M, C](
      rootPath = rootPath,
      attributeStore = attributeStore,
      rddWriter = rddWriter,
      keyIndexMethod = indexMethod
    )

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    rootPath: Path,
    rddWriter: HadoopRDDWriter[K, V],
    indexMethod: KeyIndexMethod[K])
  (implicit cons: Bridge[(RDD[(K, V)], M), C]): HadoopLayerWriter[K, V, M, C] =
    apply(
      rootPath = rootPath,
      attributeStore = HadoopAttributeStore.default(rootPath),
      rddWriter = rddWriter,
      indexMethod = indexMethod)

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    rootPath: Path,
    indexMethod: KeyIndexMethod[K])
  (implicit
    format: HadoopFormat[K, V],
    cons: Bridge[(RDD[(K, V)], M), C]): HadoopLayerWriter[K, V, M, C] =
    apply(
      rootPath = rootPath,
      rddWriter = new HadoopRDDWriter[K, V](HadoopCatalogConfig.DEFAULT),
      indexMethod = indexMethod)

  def spatial(rootPath: Path, keyIndexMethod: KeyIndexMethod[SpatialKey])
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpatialKey, Tile)], RasterMetaData), RasterRDD[SpatialKey]]) =
    new HadoopLayerWriter[SpatialKey, Tile, RasterMetaData, RasterRDD[SpatialKey]](
      rootPath,
      HadoopAttributeStore.default(rootPath),
      new HadoopRDDWriter[SpatialKey, Tile](HadoopCatalogConfig.DEFAULT), keyIndexMethod)

  def spatialMultiBand(rootPath: Path, keyIndexMethod: KeyIndexMethod[SpatialKey])
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpatialKey, MultiBandTile)], RasterMetaData), MultiBandRasterRDD[SpatialKey]]) =
    new HadoopLayerWriter[SpatialKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpatialKey]](
      rootPath,
      HadoopAttributeStore.default(rootPath),
      new HadoopRDDWriter[SpatialKey, MultiBandTile](HadoopCatalogConfig.DEFAULT), keyIndexMethod)

  def spaceTime(rootPath: Path, keyIndexMethod: KeyIndexMethod[SpaceTimeKey])
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpaceTimeKey, Tile)], RasterMetaData), RasterRDD[SpaceTimeKey]]) =
    new HadoopLayerWriter[SpaceTimeKey, Tile, RasterMetaData, RasterRDD[SpaceTimeKey]](
      rootPath,
      HadoopAttributeStore.default(rootPath),
      new HadoopRDDWriter[SpaceTimeKey, Tile](HadoopCatalogConfig.DEFAULT), keyIndexMethod)

  def spaceTimeMultiBand(rootPath: Path, keyIndexMethod: KeyIndexMethod[SpaceTimeKey])
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpaceTimeKey, MultiBandTile)], RasterMetaData), MultiBandRasterRDD[SpaceTimeKey]]) =
    new HadoopLayerWriter[SpaceTimeKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpaceTimeKey]](
      rootPath,
      HadoopAttributeStore.default(rootPath),
      new HadoopRDDWriter[SpaceTimeKey, MultiBandTile](HadoopCatalogConfig.DEFAULT), keyIndexMethod)

}