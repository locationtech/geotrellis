package geotrellis.spark.io.hadoop.spacetime

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.raster._

import org.apache.spark.{SparkContext, Logging}
import org.apache.spark.rdd.RDD
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat

// TODO: Refactor the writer and reader logic to abstract over the key type.
object SpaceTimeRasterRDDReaderProvider extends RasterRDDReaderProvider[SpaceTimeKey] with Logging {

  def index(tileLayout: TileLayout, keyBounds: KeyBounds[SpaceTimeKey]): KeyIndex[SpaceTimeKey] = {
    val lnOf2 = scala.math.log(2) // natural log of 2
    def log2(x: Double): Double = scala.math.log(x) / lnOf2
    val spatialResolution = log2(tileLayout.layoutCols).toInt

    new SpaceTimeKeyIndex(keyBounds.minKey, keyBounds.maxKey, spatialResolution, 8)
  }

  class SpaceTimeFilterMapFileInputFormat extends FilterMapFileInputFormat[SpaceTimeKeyWritable, TileWritable] {
    def createKey() = new SpaceTimeKeyWritable
    def createValue() = new TileWritable
  }

  def reader(catalogConfig: HadoopRasterCatalogConfig, layerMetaData: HadoopLayerMetaData, keyIndex: KeyIndex[SpaceTimeKey])(implicit sc: SparkContext): FilterableRasterRDDReader[SpaceTimeKey] =
    new FilterableRasterRDDReader[SpaceTimeKey] {
      def read(layerId: LayerId, filterSet: FilterSet[SpaceTimeKey]): RasterRDD[SpaceTimeKey] = {
        val path = layerMetaData.path

        val dataPath = path.suffix(catalogConfig.SEQFILE_GLOB)

        logDebug(s"Loading $layerId from $dataPath")

        val conf = sc.hadoopConfiguration
        val inputConf = conf.withInputPath(dataPath)

        val writableRdd: RDD[(SpaceTimeKeyWritable, TileWritable)] =
          if(filterSet.isEmpty) {
            sc.newAPIHadoopRDD(
              inputConf,
              classOf[SpaceTimeFilterMapFileInputFormat],
              classOf[SpaceTimeKeyWritable],
              classOf[TileWritable]
            )
          } else {
            sc.newAPIHadoopRDD(
              inputConf,
              classOf[SpaceTimeFilterMapFileInputFormat],
              classOf[SpaceTimeKeyWritable],
              classOf[TileWritable]
            )
          }

        val rasterMetaData = layerMetaData.rasterMetaData

        asRasterRDD(rasterMetaData) {
          writableRdd
            .map  { case (keyWritable, tileWritable) =>
              (keyWritable.get._2, tileWritable.toTile(rasterMetaData))
          }
        }

      }
    }
}
