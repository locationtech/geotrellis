package geotrellis.spark.io.slippy

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.io.Filesystem
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter._
import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}
import org.apache.hadoop.fs.Path

import java.io.File
import scala.collection.JavaConversions._

trait SlippyTileReader[T] {
  def read(zoom: Int)(implicit sc: SparkContext): RDD[(SpatialKey, T)]
  def read(zoom: Int, key: SpatialKey): T
  def read(zoom: Int, x: Int, y: Int): T =
    read(zoom, SpatialKey(x, y))
}

object SlippyTileReader {
  val TilePath = """.*/(\d+)/(\d+)\.\w+$""".r
}
