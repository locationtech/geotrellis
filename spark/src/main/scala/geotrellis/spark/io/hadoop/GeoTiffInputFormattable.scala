package geotrellis.spark.io.hadoop

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.spark._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.util.StreamingByteReader
import geotrellis.vector.ProjectedExtent

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.InputFormat
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import java.lang.Class
import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

trait GeoTiffInputFormattable[K, V] {
  type I <: InputFormat[K, V]

  def formatClass: Class[I]
  def keyClass: Class[K]
  def valueClass: Class[V]

  def configuration(path: Path, options: HadoopGeoTiffRDD.Options)(implicit sc: SparkContext): Configuration = {
    val withInput = sc.hadoopConfiguration.withInputDirectory(path, options.tiffExtensions)
    options.crs.foreach { crs => GeoTiffInputFormat.setCrs(withInput, crs)}
    withInput
  }

  def load(path: Path, options: HadoopGeoTiffRDD.Options)(implicit sc: SparkContext): RDD[(K, V)] = {
    val conf = configuration(path, options)
    sc.newAPIHadoopRDD(
      conf,
      formatClass,
      keyClass,
      valueClass
    )
  }

  def load(windows: RDD[(Path, GridBounds)], options: HadoopGeoTiffRDD.Options): RDD[(K, V)]
}

object SpatialSinglebandGeoTiffInputFormattable extends GeoTiffInputFormattable[ProjectedExtent, Tile] {
  type I = GeoTiffInputFormat

  def formatClass = classOf[GeoTiffInputFormat]
  def keyClass = classOf[ProjectedExtent]
  def valueClass = classOf[Tile]

  def load(windows: RDD[(Path, GridBounds)], options: HadoopGeoTiffRDD.Options): RDD[(ProjectedExtent, Tile)] = {
    val conf = new SerializableConfiguration(windows.sparkContext.hadoopConfiguration)
    windows
      .map { case (path, window) =>
        val reader = StreamingByteReader(HdfsRangeReader(path, conf.value))
        val gt = SinglebandGeoTiff.streaming(reader)
        val raster: Raster[Tile] =
          gt.raster.crop(window)

        (ProjectedExtent(raster.extent, options.crs.getOrElse(gt.crs)), raster.tile)
      }
  }
}

object SpatialMultibandGeoTiffInputFormattable extends GeoTiffInputFormattable[ProjectedExtent, MultibandTile] {
  type I = MultibandGeoTiffInputFormat

  def formatClass = classOf[MultibandGeoTiffInputFormat]
  def keyClass = classOf[ProjectedExtent]
  def valueClass = classOf[MultibandTile]

  def load(windows: RDD[(Path, GridBounds)], options: HadoopGeoTiffRDD.Options): RDD[(ProjectedExtent, MultibandTile)] = {
    val conf = new SerializableConfiguration(windows.sparkContext.hadoopConfiguration)
    windows
      .map { case (path, window) =>
        val reader = StreamingByteReader(HdfsRangeReader(path, conf.value))
        val gt = MultibandGeoTiff.streaming(reader)
        val raster: Raster[MultibandTile] =
          gt.raster.crop(window)

        (ProjectedExtent(raster.extent, options.crs.getOrElse(gt.crs)), raster.tile)
      }
  }
}

trait TemporalGeoTiffInputFormattable[T] extends GeoTiffInputFormattable[TemporalProjectedExtent, T]  {
  override def configuration(path: Path, options: HadoopGeoTiffRDD.Options)(implicit sc: SparkContext): Configuration = {
    val conf = super.configuration(path, options)

    TemporalGeoTiffInputFormat.setTimeTag(conf, options.timeTag)
    TemporalGeoTiffInputFormat.setTimeFormat(conf, options.timeFormat)
    conf
  }

  def getTime(geoTiff: GeoTiffData, options: HadoopGeoTiffRDD.Options): ZonedDateTime = {
    val timeTag = options.timeTag
    val dateTimeString = geoTiff.tags.headTags.getOrElse(timeTag, sys.error(s"There is no tag $timeTag in the GeoTiff header"))
    val formatter = DateTimeFormatter.ofPattern(options.timeFormat).withZone(ZoneOffset.UTC)
    ZonedDateTime.from(formatter.parse(dateTimeString))
  }
}

object TemporalSinglebandGeoTiffInputFormattable extends TemporalGeoTiffInputFormattable[Tile] {
  type I = TemporalGeoTiffInputFormat

  def formatClass = classOf[TemporalGeoTiffInputFormat]
  def keyClass = classOf[TemporalProjectedExtent]
  def valueClass = classOf[Tile]

  def load(windows: RDD[(Path, GridBounds)], options: HadoopGeoTiffRDD.Options): RDD[(TemporalProjectedExtent, Tile)] = {
    val conf = new SerializableConfiguration(windows.sparkContext.hadoopConfiguration)
    windows
      .map { case (path, window) =>
        val reader = StreamingByteReader(HdfsRangeReader(path, conf.value))
        val gt = SinglebandGeoTiff.streaming(reader)
        val raster: Raster[Tile] =
          gt.raster.crop(window)

        val time = getTime(gt, options)

        (TemporalProjectedExtent(raster.extent, options.crs.getOrElse(gt.crs), time), raster.tile)
      }
  }
}

object TemporalMultibandGeoTiffInputFormattable extends TemporalGeoTiffInputFormattable[MultibandTile] {
  type I = TemporalMultibandGeoTiffInputFormat

  def formatClass = classOf[TemporalMultibandGeoTiffInputFormat]
  def keyClass = classOf[TemporalProjectedExtent]
  def valueClass = classOf[MultibandTile]

  def load(windows: RDD[(Path, GridBounds)], options: HadoopGeoTiffRDD.Options): RDD[(TemporalProjectedExtent, MultibandTile)] = {
    val conf = new SerializableConfiguration(windows.sparkContext.hadoopConfiguration)
    windows
      .map { case (path, window) =>
        val reader = StreamingByteReader(HdfsRangeReader(path, conf.value))
        val gt = MultibandGeoTiff.streaming(reader)
        val raster: Raster[MultibandTile] =
          gt.raster.crop(window)

        val time = getTime(gt, options)

        (TemporalProjectedExtent(raster.extent, options.crs.getOrElse(gt.crs), time), raster.tile)
      }
  }
}
