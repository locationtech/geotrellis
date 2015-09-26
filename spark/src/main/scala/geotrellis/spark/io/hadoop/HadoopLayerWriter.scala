package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.json._
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.{LayerWriteError, ContainerConstructor, Writer}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect._

class HadoopLayerWriter[K: SpatialComponent: Boundable: JsonFormat: ClassTag, TileType: ClassTag, Container[_]](
  rootPath: Path,
  val attributeStore: HadoopAttributeStore,
  rddWriter: HadoopRDDWriter[K, TileType],
  keyIndexMethod: KeyIndexMethod[K])
(implicit val cons: ContainerConstructor[K, TileType, Container])
  extends Writer[LayerId, Container[K] with RDD[(K, TileType)]] {

  def write(id: LayerId, rdd: Container[K] with RDD[(K, TileType)]): Unit = {
    implicit val sc = rdd.sparkContext

    val layerPath = new Path(rootPath,  s"${id.name}/${id.zoom}")

    val layerMetaData =
      HadoopLayerMetaData(
        keyClass = classTag[K].toString(),
        valueClass = classTag[TileType].toString(),
        path = layerPath
      )
    val rasterMetaData = cons.getMetaData(rdd)
    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd.asInstanceOf[RDD[(K, TileType)]])
    val keyIndex = keyIndexMethod.createIndex(keyBounds)

    try {
      attributeStore.cacheWrite(id, Fields.layerMetaData, layerMetaData)
      attributeStore.cacheWrite(id, Fields.rddMetadata, rasterMetaData)(cons.metaDataFormat)
      attributeStore.cacheWrite(id, Fields.keyBounds, keyBounds)
      attributeStore.cacheWrite(id, Fields.keyIndex, keyIndex)
      // TODO: Writers need to handle Schema changes
      //attributeStore.cacheWrite(id, Fields.schema, rddWriter.schema.toString.parseJson)

      rddWriter.write(rdd, layerPath, keyIndex)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object HadoopLayerWriter {

  def apply[K: SpatialComponent: Boundable: JsonFormat: ClassTag, V: ClassTag, C[_]](
    rootPath: Path,
    attributeStore: HadoopAttributeStore,
    rddWriter: HadoopRDDWriter[K, V],
    indexMethod: KeyIndexMethod[K])
  (implicit cons: ContainerConstructor[K, V, C]): HadoopLayerWriter[K, V, C] =
    new HadoopLayerWriter[K, V, C](
      rootPath = rootPath,
      attributeStore = attributeStore,
      rddWriter = rddWriter,
      keyIndexMethod = indexMethod
    )

  def apply[K: SpatialComponent: Boundable: JsonFormat: ClassTag, V: ClassTag, C[_]](
    rootPath: Path,
    rddWriter: HadoopRDDWriter[K, V],
    indexMethod: KeyIndexMethod[K])
  (implicit sc: SparkContext, cons: ContainerConstructor[K, V, C]): HadoopLayerWriter[K, V, C] =
    apply(
      rootPath = rootPath,
      attributeStore = HadoopAttributeStore(new Path(rootPath, "attributes"), sc.hadoopConfiguration),
      rddWriter = rddWriter,
      indexMethod = indexMethod)

  def apply[K: SpatialComponent: Boundable: JsonFormat: ClassTag, V: ClassTag, C[_]](
    rootPath: Path,
    indexMethod: KeyIndexMethod[K])
  (implicit
    sc: SparkContext,
    format: HadoopFormat[K, V],
    cons: ContainerConstructor[K, V, C]): HadoopLayerWriter[K, V, C] =
    apply(
      rootPath = rootPath,
      rddWriter = new HadoopRDDWriter[K, V](HadoopCatalogConfig.DEFAULT),
      indexMethod = indexMethod)
}