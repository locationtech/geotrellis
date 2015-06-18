package geotrellis.spark.io.hadoop.spatial

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import geotrellis.raster._
import scala.reflect.ClassTag
import org.apache.spark.SparkContext

class SpatialTileReader[T: ClassTag] extends TileReader[SpatialKey, T] {

  def read(
    catalogConfig: HadoopRasterCatalogConfig,
    layerMetaData: HadoopLayerMetaData,
    index: KeyIndex[SpatialKey],
    keyBounds: KeyBounds[SpatialKey]
  )(implicit sc: SparkContext): T = {
    require(keyBounds.minKey == keyBounds.maxKey, s"TileReader expects KeyBounds for single tile, got: $keyBounds")
    
    val path = layerMetaData.path
    val dataPath = path.suffix(catalogConfig.SEQFILE_GLOB)

    val conf = sc.hadoopConfiguration
    val inputConf = conf.withInputPath(dataPath)

    val filterDefinition = (Seq(keyBounds), index.indexRanges(keyBounds).toArray)
    inputConf.setSerialized(FilterMapFileInputFormat.FILTER_INFO_KEY, filterDefinition)
    val inputFormat = new SpatialFilterMapFileInputFormat()

    sc.newAPIHadoopRDD(
      inputConf,
      classOf[SpatialFilterMapFileInputFormat[T]],
      classOf[SpatialKeyWritable],
      classOf[KryoWritable[T]]
    ).first._2.get

  }
}
