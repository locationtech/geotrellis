package geotrellis.spark.io.hadoop.spatial

import geotrellis.spark._
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
class SpatialRasterRDDReader[T: ClassTag] extends RasterRDDReader[SpatialKey, T] with Logging {

  def read(
    catalogConfig: HadoopRasterCatalogConfig,
    layerMetaData: HadoopLayerMetaData,
    keyIndex: KeyIndex[SpatialKey],
    keyBounds: KeyBounds[SpatialKey]    
  )(layerId: LayerId, queryKeyBounds: Seq[KeyBounds[SpatialKey]])
  (implicit sc: SparkContext): RasterRDD[SpatialKey, T] = {
    val path = layerMetaData.path

    val dataPath = path.suffix(catalogConfig.SEQFILE_GLOB)

    logDebug(s"Loading $layerId from $dataPath")

    val conf = sc.hadoopConfiguration
    val inputConf = conf.withInputPath(dataPath)

    val writableRdd: RDD[(SpatialKeyWritable, KryoWritable)] =
      if(Seq(keyBounds) == queryKeyBounds) {
        sc.newAPIHadoopRDD(
          inputConf,
          classOf[SequenceFileInputFormat[SpatialKeyWritable, KryoWritable]],
          classOf[SpatialKeyWritable],
          classOf[KryoWritable])
      } else {
        val ranges = queryKeyBounds.map{ keyIndex.indexRanges(_) }.flatten
        inputConf.setSerialized (FilterMapFileInputFormat.FILTER_INFO_KEY,
          (queryKeyBounds, ranges.toArray))

        sc.newAPIHadoopRDD(
          inputConf,
          classOf[SpatialFilterMapFileInputFormat],
          classOf[SpatialKeyWritable],
          classOf[KryoWritable])
      }

      val rasterMetaData = layerMetaData.rasterMetaData
      val classTagT = implicitly[ClassTag[T]]
      asRasterRDD(rasterMetaData) {
        writableRdd.map  { case (keyWritable, tileWritable) =>
          (keyWritable.get._2, tileWritable.get[T]()(classTagT))
        }
      }
  }
}
