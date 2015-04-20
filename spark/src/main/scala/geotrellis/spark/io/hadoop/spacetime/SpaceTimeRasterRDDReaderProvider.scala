package geotrellis.spark.io.hadoop.spacetime

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.spark.{SparkContext, Logging}
import org.apache.spark.rdd.RDD
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat

import scala.collection.mutable
import org.joda.time.{DateTimeZone, DateTime}

import scala.reflect._

// TODO: Refactor the writer and reader logic to abstract over the key type.
object SpaceTimeRasterRDDReaderProvider extends RasterRDDReaderProvider[SpaceTimeKey] {

  def reader(
    catalogConfig: HadoopRasterCatalogConfig,
    layerMetaData: HadoopLayerMetaData,
    keyIndex: KeyIndex[SpaceTimeKey],
    keyBounds: KeyBounds[SpaceTimeKey]
  )(implicit sc: SparkContext): FilterableRasterRDDReader[SpaceTimeKey] =
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
              classOf[SequenceFileInputFormat[SpaceTimeKeyWritable, TileWritable]],
              classOf[SpaceTimeKeyWritable],
              classOf[TileWritable]
            )
          } else {
            inputConf.setSerialized(FilterMapFileInputFormat.FILTER_INFO_KEY,
              (filterSet, FilterRanges.spatiotemporal(filterSet, keyBounds, keyIndex).toArray))

            sc.newAPIHadoopRDD(
              inputConf,
              classOf[SpaceTimeFilterMapFileInputFormat],
              classOf[SpaceTimeKeyWritable],
              classOf[TileWritable]
            )
          }

          val rasterMetaData = layerMetaData.rasterMetaData

          asRasterRDD(rasterMetaData) {
            writableRdd.map  { case (keyWritable, tileWritable) =>
              (keyWritable.get._2, tileWritable.toTile(rasterMetaData))
            }
          }
      }
  }
}
