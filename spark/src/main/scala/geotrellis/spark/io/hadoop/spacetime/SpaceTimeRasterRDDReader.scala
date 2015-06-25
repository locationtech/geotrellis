package geotrellis.spark.io.hadoop.spacetime

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
import org.joda.time.{DateTimeZone, DateTime}

import scala.reflect._

// TODO: Refactor the writer and reader logic to abstract over the key type.
class SpaceTimeRasterRDDReader[T: ClassTag] extends RasterRDDReader[SpaceTimeKey, T] with Logging {

  def read(
    catalogConfig: HadoopRasterCatalogConfig,
    layerMetaData: HadoopLayerMetaData,
    keyIndex: KeyIndex[SpaceTimeKey],
    keyBounds: KeyBounds[SpaceTimeKey]
  )(layerId: LayerId, queryKeyBounds: Seq[KeyBounds[SpaceTimeKey]])
  (implicit sc: SparkContext): RasterRDD[SpaceTimeKey, T] = {
    val path = layerMetaData.path

    val dataPath = path.suffix(catalogConfig.SEQFILE_GLOB)

    logDebug(s"Loading $layerId from $dataPath")

    val conf = sc.hadoopConfiguration
    val inputConf = conf.withInputPath(dataPath)

    val writableRdd: RDD[(SpaceTimeKeyWritable, KryoWritable)] =
      if(Seq(keyBounds) == queryKeyBounds) {
        sc.newAPIHadoopRDD(
          inputConf,
          classOf[SequenceFileInputFormat[SpaceTimeKeyWritable, KryoWritable]],
          classOf[SpaceTimeKeyWritable],
          classOf[KryoWritable]
        )
      } else {
        val ranges = queryKeyBounds.flatMap(keyIndex.indexRanges(_))
        inputConf.setSerialized (FilterMapFileInputFormat.FILTER_INFO_KEY,
          (queryKeyBounds, ranges.toArray))

        sc.newAPIHadoopRDD(
          inputConf,
          classOf[SpaceTimeFilterMapFileInputFormat],
          classOf[SpaceTimeKeyWritable],
          classOf[KryoWritable]
        )
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
