package geotrellis.spark.io.hadoop.spatial

import geotrellis.spark._
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

object SpatialRasterRDDWriterProvider extends RasterRDDWriterProvider[SpatialKey] {
  type KeyWritable = SpatialKeyWritable
  val kwOrderable = implicitly[Ordering[SpatialKeyWritable]]
  val kwCTaggable = implicitly[ClassTag[SpatialKeyWritable]]
  val applyKeyWritable = (l: Long, k: SpatialKey) => SpatialKeyWritable.apply(l, k)
}










