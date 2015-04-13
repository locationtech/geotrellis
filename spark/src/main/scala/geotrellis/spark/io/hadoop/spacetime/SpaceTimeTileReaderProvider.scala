package geotrellis.spark.io.hadoop.spacetime

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.spark.SparkContext

object SpaceTimeTileReaderProvider extends TileReaderProvider[SpaceTimeKey] {

  def index(tileLayout: TileLayout, keyBounds: KeyBounds[SpaceTimeKey]): KeyIndex[SpaceTimeKey] = {
    val lnOf2 = scala.math.log(2) // natural log of 2
    def log2(x: Double): Double = scala.math.log(x) / lnOf2
    val spatialResolution = log2(tileLayout.layoutCols).toInt

    new HilbertSpaceTimeKeyIndex(keyBounds.minKey, keyBounds.maxKey, spatialResolution, 8)
  }

  def reader(
    catalogConfig: HadoopRasterCatalogConfig,
    layerMetaData: HadoopLayerMetaData,
    index: KeyIndex[SpaceTimeKey]
  )(implicit sc: SparkContext): Reader[SpaceTimeKey, Tile] =
    new Reader[SpaceTimeKey, Tile] {
      def read(key: SpaceTimeKey) = {
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

}
