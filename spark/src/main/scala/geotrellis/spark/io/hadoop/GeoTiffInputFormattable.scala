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

/**
 * A type class that is used for determining how to read a spatial GeoTiff off Hdfs and
 * turn it into a RDD[(ProjectedExtent, V)].
 */
trait GeoTiffInputFormattable[K, V] {
  type I <: InputFormat[K, V]

  def formatClass: Class[I]
  def keyClass: Class[K]
  def valueClass: Class[V]

  /**
   * Creates a Configuraiton for a Spark Job based on the options based in.
   *
   * @param path: A Path that is the file path to the file on Hdfs.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return A [[Configuration]] that is used to create RDDs in a given way.
   */
  def configuration(path: Path, options: HadoopGeoTiffRDD.Options)(implicit sc: SparkContext): Configuration = {
    val withInput = sc.hadoopConfiguration.withInputDirectory(path, options.tiffExtensions)
    options.crs.foreach { crs => GeoTiffInputFormat.setCrs(withInput, crs)}
    withInput
  }

  /**
   * Creates a RDD[(K, V)] whose K and V depends on the type of the GeoTiff that is going to be read in.
   *
   * @param path: A Path that is the file path to the file on Hdfs.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return a RDD with key and value pair of type K and V, respectively.
   */
  def load(path: Path, options: HadoopGeoTiffRDD.Options)(implicit sc: SparkContext): RDD[(K, V)] = {
    val conf = configuration(path, options)
    sc.newAPIHadoopRDD(
      conf,
      formatClass,
      keyClass,
      valueClass
    )
  }

  /**
   * Creates a RDD[(K, V)] whose K and V depends on the type of the GeoTiff that is going to be read in.
   *
   * @param windows: An RDD that contains a [[Path]] and the [[GridBounds]] which the
   *   resulting tile will be constrained to.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return a RDD with key and value pair of type K and V, respectively.
   */
  def load(windows: RDD[(Path, GridBounds)], options: HadoopGeoTiffRDD.Options): RDD[(K, V)]
}

/**
 * This object extends [[GeotiffInputFormattable]] and is used to create RDDs of [[ProjectedExtent]]s and
 * [[Tile]]s.
 */
object SpatialSinglebandGeoTiffInputFormattable extends GeoTiffInputFormattable[ProjectedExtent, Tile] {
  type I = GeoTiffInputFormat

  def formatClass = classOf[GeoTiffInputFormat]
  def keyClass = classOf[ProjectedExtent]
  def valueClass = classOf[Tile]

  /**
   * Creates a RDD[(ProjectedExtent, Tile)] via the parameters set by the options.
   *
   * @param windows: An RDD that contains a [[Path]] and the [[GridBounds]] which the
   *   resulting tile will be constrained to.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return A RDD with key value paris of type [[ProjectedExtent]] and [[Tile]], respectively.
   */
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

/**
 * This object extends [[GeotiffInputFormattable]] and is used to create RDDs of [[ProjectedExtent]]s and
 * [[MultibandTile]]s.
 */
object SpatialMultibandGeoTiffInputFormattable extends GeoTiffInputFormattable[ProjectedExtent, MultibandTile] {
  type I = MultibandGeoTiffInputFormat

  def formatClass = classOf[MultibandGeoTiffInputFormat]
  def keyClass = classOf[ProjectedExtent]
  def valueClass = classOf[MultibandTile]

  /**
   * Creates a RDD[(ProjectedExtent, MultibandTile)] via the parameters set by the options.
   *
   * @param windows: An RDD that contains a [[Path]] and the [[GridBounds]] which the
   *   resulting tile will be constrained to.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return A RDD with key value paris of type [[ProjectedExtent]] and [[MultibandTile]], respectively.
   */
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

/**
 * A type class that extends [[GeoTiffInputFormattable]] and is used for
 * determining how to read a temporal-spatial GeoTiff off Hdfs and turn it into a
 * RDD[(TemporalProjectedExtent, V)].
 */
trait TemporalGeoTiffInputFormattable[T] extends GeoTiffInputFormattable[TemporalProjectedExtent, T]  {
  override def configuration(path: Path, options: HadoopGeoTiffRDD.Options)(implicit sc: SparkContext): Configuration = {
    val conf = super.configuration(path, options)

    TemporalGeoTiffInputFormat.setTimeTag(conf, options.timeTag)
    TemporalGeoTiffInputFormat.setTimeFormat(conf, options.timeFormat)
    conf
  }

  /**
   * Gets the time that a given GeoTiff has attributed to it.
   *
   * @param geoTiff: [[GeoTiffData]] of a given GeoTiff.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return The [[ZondedDatTime]] property of the GeoTiff.
   */
  def getTime(geoTiff: GeoTiffData, options: HadoopGeoTiffRDD.Options): ZonedDateTime = {
    val timeTag = options.timeTag
    val dateTimeString = geoTiff.tags.headTags.getOrElse(timeTag, sys.error(s"There is no tag $timeTag in the GeoTiff header"))
    val formatter = DateTimeFormatter.ofPattern(options.timeFormat).withZone(ZoneOffset.UTC)
    ZonedDateTime.from(formatter.parse(dateTimeString))
  }
}

/**
 * This object extends [[TemporalGeotiffInputFormattable]] and is used to create RDDs of [[TemporalProjectedExtent]]s and
 * [[Tile]]s.
 */
object TemporalSinglebandGeoTiffInputFormattable extends TemporalGeoTiffInputFormattable[Tile] {
  type I = TemporalGeoTiffInputFormat

  def formatClass = classOf[TemporalGeoTiffInputFormat]
  def keyClass = classOf[TemporalProjectedExtent]
  def valueClass = classOf[Tile]

  /**
   * Creates a RDD[(TemporalProjectedExtent, Tile)] via the parameters set by the options.
   *
   * @param windows: An RDD that contains a [[Path]] and the [[GridBounds]] which the
   *   resulting tile will be constrained to.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return A RDD with key value paris of type [[TemporalProjectedExtent]] and [[Tile]], respectively.
   */
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

/**
 * This object extends [[TemporalGeotiffInputFormattable]] and is used to create RDDs of [[TemporalProjectedExtent]]s and
 * [[MultibandTile]]s.
 */
object TemporalMultibandGeoTiffInputFormattable extends TemporalGeoTiffInputFormattable[MultibandTile] {
  type I = TemporalMultibandGeoTiffInputFormat

  def formatClass = classOf[TemporalMultibandGeoTiffInputFormat]
  def keyClass = classOf[TemporalProjectedExtent]
  def valueClass = classOf[MultibandTile]

  /**
   * Creates a RDD[(TemporalProjectedExtent, MultibandTile)] via the parameters set by the options.
   *
   * @param windows: An RDD that contains a [[Path]] and the [[GridBounds]] which the
   *   resulting tile will be constrained to.
   * @param options: An instance of [[Options]] that contains any user defined or defualt settings.
   *
   * @return A RDD with key value paris of type [[TemporalProjectedExtent]] and [[MultibandTile]], respectively.
   */
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
