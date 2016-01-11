package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop.formats.FilterMapFileInputFormat
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect.ClassTag

class HadoopTileReader[K: JsonFormat: ClassTag, V, I <: KeyIndex[K]: JsonFormat](val attributeStore: HadoopAttributeStore)
    (implicit format: HadoopFormat[K, V], sc: SparkContext) extends Reader[LayerId, Reader[K, V]] {

  val catalogConfig = HadoopCatalogConfig.DEFAULT
  val conf = attributeStore.hadoopConfiguration

  def read(layerId: LayerId): Reader[K, V] = new Reader[K, V] {

    val (layerMetaData, _, _, keyIndex, _) =
      attributeStore.readLayerAttributes[HadoopLayerHeader, Unit, Unit, I, Unit](layerId)

    val dataPath = layerMetaData.path.suffix(catalogConfig.SEQFILE_GLOB)
    val inputConf = conf.withInputPath(dataPath)
    
    def read(key: K): V = {
      val keyBounds = KeyBounds[K](key, key)
      val filterDefinition = (Seq(keyBounds), keyIndex.indexRanges(keyBounds).toArray)
      inputConf.setSerialized(FilterMapFileInputFormat.FILTER_INFO_KEY, filterDefinition)

      // TODO: There must be a way to do this through Readers, which must be faster
      sc.newAPIHadoopRDD(
        inputConf,
        format.filteredInputFormatClass,
        format.kClass,
        format.vClass
      ).first()._2.get()
    }
  }
}

object HadoopTileReader {
  def custom[K: JsonFormat: ClassTag, V, I <: KeyIndex[K]: JsonFormat](attributeStore: HadoopAttributeStore)(implicit format: HadoopFormat[K, V], sc: SparkContext): HadoopTileReader[K, V, I] =
    new HadoopTileReader[K, V, I](attributeStore)

  def custom[K: JsonFormat: ClassTag, V, I <: KeyIndex[K]: JsonFormat](rootPath: Path)(implicit format: HadoopFormat[K, V], sc: SparkContext): HadoopTileReader[K, V, I] =
    custom(HadoopAttributeStore(new Path(rootPath, "attributes")))

  def apply[K: JsonFormat: ClassTag, V](attributeStore: HadoopAttributeStore)(implicit format: HadoopFormat[K, V], sc: SparkContext): HadoopTileReader[K, V, KeyIndex[K]] =
    new HadoopTileReader[K, V, KeyIndex[K]](attributeStore)

  def apply[K: JsonFormat: ClassTag, V](rootPath: Path)(implicit format: HadoopFormat[K, V], sc: SparkContext): HadoopTileReader[K, V, KeyIndex[K]] =
    apply(HadoopAttributeStore(new Path(rootPath, "attributes")))
}