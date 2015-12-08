package geotrellis.spark.io.slippy

import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.io.Filesystem
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
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

import java.io._
import scala.collection.JavaConversions._

class FileSlippyTileReader[T](uri: String, extensions: Seq[String] = Seq())(fromBytes: (SpatialKey, Array[Byte]) => T) extends SlippyTileReader[T] {
  import SlippyTileReader.TilePath

  private def listFiles(path: String): Seq[File] =
    listFiles(new File(path))

  private def listFiles(file: File): Seq[File] =
    if(extensions.isEmpty) { FileUtils.listFiles(file, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).toSeq }
    else { FileUtils.listFiles(file, new SuffixFileFilter(extensions), TrueFileFilter.INSTANCE).toSeq }

  def read(zoom: Int, key: SpatialKey): T = {
    val dir = new File(uri, s"$zoom/${key.col}/")

    val lFromBytes = fromBytes
    listFiles(dir).filter { f => f.getName.startsWith(s"${key.row}") } match {
      case Seq() => throw new FileNotFoundException(s"${dir}/${key.row}*")
      case Seq(tilePath) => lFromBytes(key, Filesystem.slurp(tilePath.getAbsolutePath))
      case _ => throw new IllegalArgumentException(s"More than one file matches path ${dir}/${key.row}*")
    }
  }

  def read(zoom: Int)(implicit sc: SparkContext): RDD[(SpatialKey, T)] = {
    val paths = {
      listFiles(new File(uri, zoom.toString).getPath)
        .flatMap { file =>
          val path = file.getAbsolutePath
          path match {
            case TilePath(x, y) => Some((SpatialKey(x.toInt, y.toInt), path))
            case _ => None
          }
        }
    }

    val lFromBytes = fromBytes
    val numPartitions = math.min(paths.size, math.max(paths.size / 10, 50)).toInt
    sc.parallelize(paths.toSeq)
      .partitionBy(new HashPartitioner(numPartitions))
      .mapPartitions({ partition =>
        partition.map { case (key, path) => (key, lFromBytes(key, Filesystem.slurp(path))) }
      }, true)
  }
}
