package geotrellis.spark.streaming.io.hadoop

import geotrellis.raster._
import geotrellis.spark.TemporalProjectedExtent
import geotrellis.vector._
import geotrellis.spark.io.hadoop.formats._

import org.apache.hadoop.conf.Configuration
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.hadoop.fs.Path
import org.apache.spark.streaming.dstream.InputDStream

trait HadoopStreamingContextMethods {
  val ssc: StreamingContext
  val sc: SparkContext = ssc.sparkContext
  val defaultTiffExtensions: Seq[String] = Seq(".tif", ".TIF", ".tiff", ".TIFF")

  def hadoopGeoTiffDStream(
    path: Path,
    filter: Path => Boolean = (p: Path) => defaultTiffExtensions.exists(p.toString.endsWith),
    newFilesOnly: Boolean = false,
    conf: Configuration = sc.hadoopConfiguration
  ): InputDStream[(ProjectedExtent, Tile)] = {
    ssc.fileStream[ProjectedExtent, Tile, GeotiffInputFormat](
      directory    = path.toUri.toString,
      filter       = filter,
      newFilesOnly = newFilesOnly,
      conf         = conf
    )
  }

  def hadoopTemporalGeoTiffDStream(
    path: Path,
    filter: Path => Boolean = (p: Path) => defaultTiffExtensions.exists(p.toString.endsWith),
    newFilesOnly: Boolean = false,
    timeTag: String = TemporalGeoTiffInputFormat.GEOTIFF_TIME_TAG_DEFAULT,
    timeFormat: String = TemporalGeoTiffInputFormat.GEOTIFF_TIME_FORMAT_DEFAULT,
    conf: Configuration = sc.hadoopConfiguration
  ): InputDStream[(TemporalProjectedExtent, Tile)] = {
    TemporalGeoTiffInputFormat.setTimeTag(conf, timeTag)
    TemporalGeoTiffInputFormat.setTimeFormat(conf, timeFormat)

    ssc.fileStream[TemporalProjectedExtent, Tile, TemporalGeoTiffInputFormat](
      directory    = path.toUri.toString,
      filter       = filter,
      newFilesOnly = newFilesOnly,
      conf         = conf
    )
  }

  def hadoopMultibandGeoTiffDStream(
    path: Path,
    filter: Path => Boolean = (p: Path) => defaultTiffExtensions.exists(p.toString.endsWith),
    newFilesOnly: Boolean = false,
    conf: Configuration = sc.hadoopConfiguration
  ): InputDStream[(ProjectedExtent, MultibandTile)] = {
    ssc.fileStream[ProjectedExtent, MultibandTile, MultibandGeoTiffInputFormat](
      directory    = path.toUri.toString,
      filter       = filter,
      newFilesOnly = newFilesOnly,
      conf         = conf
    )
  }

  def hadoopTemporalMultibandGeoTiffDStream(
    path: Path,
    filter: Path => Boolean = (p: Path) => defaultTiffExtensions.exists(p.toString.endsWith),
    newFilesOnly: Boolean = false,
    timeTag: String = TemporalGeoTiffInputFormat.GEOTIFF_TIME_TAG_DEFAULT,
    timeFormat: String = TemporalGeoTiffInputFormat.GEOTIFF_TIME_FORMAT_DEFAULT,
    conf: Configuration = sc.hadoopConfiguration
  ): InputDStream[(TemporalProjectedExtent, MultibandTile)] = {
    TemporalGeoTiffInputFormat.setTimeTag(conf, timeTag)
    TemporalGeoTiffInputFormat.setTimeFormat(conf, timeFormat)

    ssc.fileStream[TemporalProjectedExtent, MultibandTile, TemporalMultibandGeoTiffInputFormat](
      directory    = path.toUri.toString,
      filter       = filter,
      newFilesOnly = newFilesOnly,
      conf         = conf
    )
  }
}

