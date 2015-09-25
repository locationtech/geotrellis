package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import spray.json.JsonFormat
import scala.reflect.ClassTag

class HadoopTileReader[K: JsonFormat: ClassTag, V](
  val attributeStore: HadoopAttributeStore,
  layerId: LayerId)
(implicit format: HadoopFormat[K, V], sc: SparkContext) extends Reader[K, V] {

  val catalogConfig = HadoopCatalogConfig.DEFAULT
  val conf = attributeStore.hadoopConfiguration
  val layerMetaData  = attributeStore.cacheRead[HadoopLayerMetaData](layerId, Fields.layerMetaData)
  val keyIndex  = attributeStore.cacheRead[KeyIndex[K]](layerId, Fields.keyIndex)

  val dataPath = layerMetaData.path.suffix(catalogConfig.SEQFILE_GLOB)
  val inputConf = conf.withInputPath(dataPath)

  def read(key: K): V = {
    // TODO: There must be a way to do this through Readers, which must be faster
    val keyBounds = KeyBounds[K](key, key)
    val filterDefinition = (Seq(keyBounds), keyIndex.indexRanges(keyBounds).toArray)
    inputConf.setSerialized(FilterMapFileInputFormat.FILTER_INFO_KEY, filterDefinition)

    sc.newAPIHadoopRDD(
      inputConf,
      format.filteredInputFormatClass,
      format.kClass,
      format.vClass
    ).first._2.get()
  }
}

object HadoopTileReader {
  def apply[K: JsonFormat: ClassTag, V](attributeStore: HadoopAttributeStore, id: LayerId)(implicit format: HadoopFormat[K, V], sc: SparkContext): HadoopTileReader[K, V] =
    new HadoopTileReader[K, V](attributeStore, id)

  def apply[K: JsonFormat: ClassTag, V](rootPath: Path, id: LayerId)(implicit format: HadoopFormat[K, V], sc: SparkContext): HadoopTileReader[K, V] =
    apply(HadoopAttributeStore(rootPath), id)
}