package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.utils._
import geotrellis.spark.io.hadoop.formats._

import geotrellis.raster._
import geotrellis.vector.Extent

import geotrellis.proj4._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapred.JobConf
import org.apache.hadoop.mapred.MapFileOutputFormat
import org.apache.hadoop.mapred.SequenceFileOutputFormat
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.Logging
import org.apache.commons.codec.binary.Base64

import java.io.PrintWriter
import java.nio.ByteBuffer
import geotrellis.spark.tiling._

import scala.reflect._

package object hadoop {
  implicit object SpatialKeyHadoopWritable extends HadoopWritable[SpatialKey] {
    type Writable = SpatialKeyWritable
    val writableClassTag = classTag[SpatialKeyWritable]
    def toWritable(key: SpatialKey) = SpatialKeyWritable(key)
    def toValue(writable: SpatialKeyWritable) = writable.get
  }

  implicit class HadoopSparkContextMethodsWrapper(val sc: SparkContext) extends HadoopSparkContextMethods

  implicit class SaveRasterMethodsWrapper[K: HadoopWritable](val rdd: RasterRDD[K]) extends SaveRasterMethods[K]

  implicit class HadoopConfigurationWrapper(config: Configuration) {
    def withInputPath(path: Path): Configuration = {
      val job = Job.getInstance(config)
      FileInputFormat.addInputPath(job, path)
      job.getConfiguration
    }

    /** Creates a Configuration with all files in a directory (recursively searched)*/
    def withInputDirectory(path: Path): Configuration = {
      val allFiles = HdfsUtils.listFiles(path, config)
      HdfsUtils.putFilesInConf(allFiles.mkString(","), config)
    }
  }
}
