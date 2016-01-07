package geotrellis.spark.io.hadoop

import geotrellis.raster.{MultiBandTile, Tile}
import geotrellis.spark.io.json._
import geotrellis.spark._
import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import geotrellis.spark.io._

import org.apache.avro.Schema
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect._

class HadoopLayerWriter[
  K: Boundable: JsonFormat: ClassTag, V: ClassTag,
  M: JsonFormat, I <: KeyIndex[K]: JsonFormat](
  rootPath: Path,
  val attributeStore: AttributeStore[JsonFormat],
  rddWriter: HadoopRDDWriter[K, V],
  keyIndexMethod: KeyIndexMethod[K, I])
  extends Writer[LayerId, RDD[(K, V)] with Metadata[M]] {

  def write(id: LayerId, rdd: RDD[(K, V)] with Metadata[M]): Unit = {
    implicit val sc = rdd.sparkContext

    val layerPath = new Path(rootPath,  s"${id.name}/${id.zoom}")

    val header =
      HadoopLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        path = layerPath
      )
    val metaData = rdd.metadata
    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
    val keyIndex = keyIndexMethod.createIndex(keyBounds)

    try {
      attributeStore.writeLayerAttributes(id, header, metaData, keyBounds, keyIndex, Option.empty[Schema])
      println(s"keyIndex: ${keyIndex}")
      // TODO: Writers need to handle Schema changes

      rddWriter.write(rdd, layerPath, keyIndex)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object HadoopLayerWriter {
  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, I <: KeyIndex[K]: JsonFormat](
    rootPath: Path, attributeStore: HadoopAttributeStore,
    rddWriter: HadoopRDDWriter[K, V], indexMethod: KeyIndexMethod[K, I]): HadoopLayerWriter[K, V, M, I] =
    new HadoopLayerWriter[K, V, M, I](
      rootPath = rootPath,
      attributeStore = attributeStore,
      rddWriter = rddWriter,
      keyIndexMethod = indexMethod
    )

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, I <: KeyIndex[K]: JsonFormat]
    (rootPath: Path, rddWriter: HadoopRDDWriter[K, V], indexMethod: KeyIndexMethod[K, I]): HadoopLayerWriter[K, V, M, I] =
    apply(
      rootPath = rootPath,
      attributeStore = HadoopAttributeStore.default(rootPath),
      rddWriter = rddWriter,
      indexMethod = indexMethod
    )

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, I <: KeyIndex[K]: JsonFormat]
    (rootPath: Path, indexMethod: KeyIndexMethod[K, I])(implicit format: HadoopFormat[K, V]): HadoopLayerWriter[K, V, M, I] =
    apply(
      rootPath = rootPath,
      rddWriter = new HadoopRDDWriter[K, V](HadoopCatalogConfig.DEFAULT),
      indexMethod = indexMethod
    )

  def spatial[I <: KeyIndex[SpatialKey]: JsonFormat](rootPath: Path, keyIndexMethod: KeyIndexMethod[SpatialKey, I])(implicit sc: SparkContext) =
    apply[SpatialKey, Tile, RasterMetaData, I](rootPath, keyIndexMethod)

  def spatialMultiBand[I <: KeyIndex[SpatialKey]: JsonFormat](rootPath: Path, keyIndexMethod: KeyIndexMethod[SpatialKey, I])(implicit sc: SparkContext) =
    apply[SpatialKey, MultiBandTile, RasterMetaData, I](rootPath, keyIndexMethod)

  def spaceTime[I <: KeyIndex[SpaceTimeKey]: JsonFormat](rootPath: Path, keyIndexMethod: KeyIndexMethod[SpaceTimeKey, I])(implicit sc: SparkContext) =
    apply[SpaceTimeKey, Tile, RasterMetaData, I](rootPath, keyIndexMethod)

  def spaceTimeMultiBand[I <: KeyIndex[SpaceTimeKey]: JsonFormat](rootPath: Path, keyIndexMethod: KeyIndexMethod[SpaceTimeKey, I])(implicit sc: SparkContext) =
    apply[SpaceTimeKey, MultiBandTile, RasterMetaData, I](rootPath, keyIndexMethod)

}
