package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.utils._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.hadoop.fs.Path
import org.apache.spark.{SparkContext, Logging}
import org.apache.spark.SparkContext._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapreduce.lib.output.{MapFileOutputFormat, SequenceFileOutputFormat}
import org.apache.hadoop.mapreduce.Job

import scala.reflect._

trait RasterRDDWriter[Key] extends Logging {
  def write(
    catalogConfig: HadoopRasterCatalogConfig,
    layerMetaData: HadoopLayerMetaData,
    keyIndex: KeyIndex[Key],
    clobber: Boolean = true)
  (layerId: LayerId, rdd: RasterRDD[Key])
  (implicit sc: SparkContext): Unit
}
