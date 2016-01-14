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

class HadoopLayerWriter[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
  rootPath: Path,
  val attributeStore: AttributeStore[JsonFormat],
  rddWriter: HadoopRDDWriter[K, V])
  extends Writer[LayerId, K, RDD[(K, V)] with Metadata[M]] {

  def write[I <: KeyIndex[K]: JsonFormat](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: I): Unit = {
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

    try {
      attributeStore.writeLayerAttributes(id, header, metaData, keyBounds, keyIndex, Option.empty[Schema])
      // TODO: Writers need to handle Schema changes

      rddWriter.write(rdd, layerPath, keyIndex)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }

  def write(id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndexMethod: KeyIndexMethod[K]): Unit = {
    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
    write(id, rdd, keyIndexMethod.createIndex(keyBounds))
  }
}

object HadoopLayerWriter {
  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
    rootPath: Path, attributeStore: HadoopAttributeStore,
    rddWriter: HadoopRDDWriter[K, V]): HadoopLayerWriter[K, V, M] =
    new HadoopLayerWriter[K, V, M](
      rootPath = rootPath,
      attributeStore = attributeStore,
      rddWriter = rddWriter
    )

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
    rootPath: Path, rddWriter: HadoopRDDWriter[K, V]): HadoopLayerWriter[K, V, M] =
    apply(rootPath, HadoopAttributeStore.default(rootPath), rddWriter)

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
    rootPath: Path)(implicit format: HadoopFormat[K, V]): HadoopLayerWriter[K, V, M] =
    apply(rootPath, new HadoopRDDWriter[K, V](HadoopCatalogConfig.DEFAULT))

  def spatial(rootPath: Path)(implicit sc: SparkContext) =
    apply[SpatialKey, Tile, RasterMetaData](rootPath)

  def spatialMultiBand(rootPath: Path)(implicit sc: SparkContext) =
    apply[SpatialKey, MultiBandTile, RasterMetaData](rootPath)

  def spaceTime(rootPath: Path)(implicit sc: SparkContext) =
    apply[SpaceTimeKey, Tile, RasterMetaData](rootPath)

  def spaceTimeMultiBand(rootPath: Path)(implicit sc: SparkContext) =
    apply[SpaceTimeKey, MultiBandTile, RasterMetaData](rootPath)

}
