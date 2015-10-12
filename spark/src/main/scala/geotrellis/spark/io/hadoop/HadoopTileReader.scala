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

class HadoopTileReader[K: JsonFormat: ClassTag, V](val attributeStore: HadoopAttributeStore)
    (implicit format: HadoopFormat[K, V], sc: SparkContext) extends Reader[LayerId, Reader[K, V]] {

  val catalogConfig = HadoopCatalogConfig.DEFAULT
  val conf = attributeStore.hadoopConfiguration

  def read(layerId: LayerId): Reader[K, V] = new Reader[K, V] {

    val (layerMetaData, _, _, keyIndex, _) =
      attributeStore.readLayerAttributes[HadoopLayerHeader, Unit, Unit, KeyIndex[K], Unit](layerId)

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
  def apply[K: JsonFormat: ClassTag, V](attributeStore: HadoopAttributeStore)(implicit format: HadoopFormat[K, V], sc: SparkContext): HadoopTileReader[K, V] =
    new HadoopTileReader[K, V](attributeStore)

  def apply[K: JsonFormat: ClassTag, V](rootPath: Path)(implicit format: HadoopFormat[K, V], sc: SparkContext): HadoopTileReader[K, V] =
    apply(HadoopAttributeStore(new Path(rootPath, "attributes")))
}