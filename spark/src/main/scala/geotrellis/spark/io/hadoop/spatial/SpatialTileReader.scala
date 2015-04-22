package geotrellis.spark.io.hadoop.spatial

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.spark.SparkContext

object SpatialTileReader extends TileReader[SpatialKey] {

  def read(
    catalogConfig: HadoopRasterCatalogConfig,
    layerMetaData: HadoopLayerMetaData,
    index: KeyIndex[SpatialKey]
  )(key: SpatialKey)(implicit sc: SparkContext): Tile = {
    val path = layerMetaData.path
    val dataPath = path.suffix(catalogConfig.SEQFILE_GLOB)

    val conf = sc.hadoopConfiguration
    val inputConf = conf.withInputPath(dataPath)

    val filterSet = new FilterSet[SpatialKey] withFilter SpaceFilter(key)
    val i = index.toIndex(key)
    val filterDefinition = (filterSet, Array((i,i)))
    inputConf.setSerialized(FilterMapFileInputFormat.FILTER_INFO_KEY, filterDefinition)
    val inputFormat = new SpatialFilterMapFileInputFormat()

    sc.newAPIHadoopRDD(
      inputConf,
      classOf[SpatialFilterMapFileInputFormat],
      classOf[SpatialKeyWritable],
      classOf[TileWritable]
    ).first._2.toTile(layerMetaData.rasterMetaData)

  }
}
