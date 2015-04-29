package geotrellis.spark.io.hadoop.spacetime

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.spark.SparkContext

object SpaceTimeTileReader extends TileReader[SpaceTimeKey] {

  def read(
    catalogConfig: HadoopRasterCatalogConfig,
    layerMetaData: HadoopLayerMetaData,
    index: KeyIndex[SpaceTimeKey]
  )(key: SpaceTimeKey)(implicit sc: SparkContext): Tile = {
    val path = layerMetaData.path
    val dataPath = path.suffix(catalogConfig.SEQFILE_GLOB)

    val conf = sc.hadoopConfiguration
    val inputConf = conf.withInputPath(dataPath)

    val filterSet = new FilterSet[SpaceTimeKey] withFilter SpaceFilter(key) withFilter TimeFilter(key.temporalKey)
    val i = index.toIndex(key)
    val filterDefinition = (filterSet, Array((i,i)))
    inputConf.setSerialized(FilterMapFileInputFormat.FILTER_INFO_KEY, filterDefinition)
    val inputFormat = new SpaceTimeFilterMapFileInputFormat()

    sc.newAPIHadoopRDD(
      inputConf,
      classOf[SpaceTimeFilterMapFileInputFormat],
      classOf[SpaceTimeKeyWritable],
      classOf[TileWritable]
    ).first._2.toTile(layerMetaData.rasterMetaData)

  }
}
