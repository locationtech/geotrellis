package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.MapFile
import org.apache.spark.SparkContext
import spray.json.JsonFormat
import scala.reflect.ClassTag

class HadoopTileReader[K: JsonFormat: ClassTag, V](
  val attributeStore: HadoopAttributeStore,
  layerId: LayerId)
(implicit format: HadoopFormat[K, V]) extends Reader[K, V] {

  val catalogConfig = HadoopCatalogConfig.DEFAULT
  val conf = attributeStore.hadoopConfiguration
  val layerMetaData  = attributeStore.cacheRead[HadoopLayerMetaData](layerId, Fields.layerMetaData)
  val keyIndex  = attributeStore.cacheRead[KeyIndex[K]](layerId, Fields.keyIndex)


  val mapFilePath: Path = layerMetaData.path.suffix(catalogConfig.SEQFILE_GLOB).getParent
  val reader = new MapFile.Reader(mapFilePath, conf)

  def read(key: K): V = {
    val kw = format.kClass.newInstance()
    val vw = format.vClass.newInstance()
    kw.set(keyIndex.toIndex(key), key)
    reader.get(kw, vw)
    vw.get()
  }
}

object HadoopTileReader {
  def apply[K: JsonFormat: ClassTag, V](attributeStore: HadoopAttributeStore, id: LayerId)(implicit format: HadoopFormat[K, V]): HadoopTileReader[K, V] =
    new HadoopTileReader[K, V](attributeStore, id)

  def apply[K: JsonFormat: ClassTag, V](rootPath: Path, id: LayerId)(implicit format: HadoopFormat[K, V], sc: SparkContext): HadoopTileReader[K, V] =
    apply(HadoopAttributeStore(rootPath), id)
}