package geotrellis.spark.io.hadoop.spacetime

import geotrellis.spark._
import geotrellis.spark.utils._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.spark.{SparkContext, Logging}
import org.apache.spark.SparkContext._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.SequenceFile
import org.apache.hadoop.mapreduce.lib.output.{MapFileOutputFormat, SequenceFileOutputFormat}
import org.apache.hadoop.mapreduce.Job

import scala.reflect.ClassTag

object SpaceTimeRasterRDDWriterProvider extends RasterRDDWriterProvider[SpaceTimeKey] {
  type KeyWritable = SpaceTimeKeyWritable
  val kwOrderable = implicitly[Ordering[SpaceTimeKeyWritable]]
  val kwCTaggable = implicitly[ClassTag[SpaceTimeKeyWritable]]
  val applyKeyWritable = (l: Long, k: SpaceTimeKey) => SpaceTimeKeyWritable.apply(l, k)
}


