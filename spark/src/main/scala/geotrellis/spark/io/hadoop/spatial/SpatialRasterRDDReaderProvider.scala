package geotrellis.spark.io.hadoop.spatial

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

import scala.reflect._

// TODO: Refactor the writer and reader logic to abstract over the key type.
object SpatialRasterRDDReaderProvider extends RasterRDDReaderProvider[SpatialKey] {

  def reader(
    catalogConfig: HadoopRasterCatalogConfig,
    layerMetaData: HadoopLayerMetaData,
    keyIndex: KeyIndex[SpatialKey],
    keyBounds: KeyBounds[SpatialKey]
  )(implicit sc: SparkContext): FilterableRasterRDDReader[SpatialKey] =
    new FilterableRasterRDDReader[SpatialKey] {
      def read(layerId: LayerId, filterSet: FilterSet[SpatialKey]): RasterRDD[SpatialKey] = {
        val path = layerMetaData.path

        val dataPath = path.suffix(catalogConfig.SEQFILE_GLOB)

        logDebug(s"Loading $layerId from $dataPath")

        val conf = sc.hadoopConfiguration
        val inputConf = conf.withInputPath(dataPath)

        val writableRdd: RDD[(SpatialKeyWritable, TileWritable)] =
          if(filterSet.isEmpty) {
            sc.newAPIHadoopRDD(
              inputConf,
              classOf[SequenceFileInputFormat[SpatialKeyWritable, TileWritable]],
              classOf[SpatialKeyWritable],
              classOf[TileWritable]
            )
          } else {
            inputConf.setSerialized(FilterMapFileInputFormat.FILTER_INFO_KEY,
              (filterSet, FilterRanges.spatial(filterSet, keyBounds, keyIndex).toArray))

            sc.newAPIHadoopRDD(
              inputConf,
              classOf[SpatialFilterMapFileInputFormat],
              classOf[SpatialKeyWritable],
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
