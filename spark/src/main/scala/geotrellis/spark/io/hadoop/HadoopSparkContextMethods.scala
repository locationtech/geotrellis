package geotrellis.spark.io.hadoop

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.spark.TemporalProjectedExtent
import geotrellis.spark.io.hadoop.formats._
import geotrellis.vector._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.conf.Configuration

trait HadoopSparkContextMethods {
  val sc: SparkContext

  def hadoopGeoTiffRDD(path: String): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(new Path(path), HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions)

  def hadoopGeoTiffRDD(path: String, tiffExtension: String): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(new Path(path), Seq(tiffExtension))

  def hadoopGeoTiffRDD(path: String, tiffExtensions: Seq[String]): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(new Path(path), tiffExtensions)

  def hadoopGeoTiffRDD(path: Path): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(path, HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions)

  def hadoopGeoTiffRDD(path: Path, tiffExtension: String): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(path, Seq(tiffExtension))

  def hadoopGeoTiffRDD(path: Path, tiffExtensions: Seq[String], crs: Option[CRS] = None): RDD[(ProjectedExtent, Tile)] = {
    val conf = sc.hadoopConfiguration.withInputDirectory(path, tiffExtensions)
    crs.foreach(GeoTiffInputFormat.setCrs(conf, _))
    sc.newAPIHadoopRDD(
      conf,
      classOf[GeoTiffInputFormat],
      classOf[ProjectedExtent],
      classOf[Tile]
    )
  }

  def hadoopTemporalGeoTiffRDD(path: String): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(new Path(path), HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions)

  def hadoopTemporalGeoTiffRDD(path: String, tiffExtension: String): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(new Path(path), Seq(tiffExtension))

  def hadoopTemporalGeoTiffRDD(path: String, tiffExtensions: Seq[String] ): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(new Path(path), tiffExtensions)

  def hadoopTemporalGeoTiffRDD(path: Path): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(path, HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions)

  def hadoopTemporalGeoTiffRDD(path: Path, tiffExtension: String): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopTemporalGeoTiffRDD(path, Seq(tiffExtension))

  def hadoopTemporalGeoTiffRDD(
    path: Path,
    tiffExtensions: Seq[String] = HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions,
    timeTag: String = TemporalGeoTiffInputFormat.GEOTIFF_TIME_TAG_DEFAULT,
    timeFormat: String = TemporalGeoTiffInputFormat.GEOTIFF_TIME_FORMAT_DEFAULT,
    crs: Option[CRS] = None
  ): RDD[(TemporalProjectedExtent, Tile)] = {
    val conf = sc.hadoopConfiguration.withInputDirectory(path, tiffExtensions)
    TemporalGeoTiffInputFormat.setTimeTag(conf, timeTag)
    TemporalGeoTiffInputFormat.setTimeFormat(conf, timeFormat)
    crs.foreach(GeoTiffInputFormat.setCrs(conf, _))
    sc.newAPIHadoopRDD(
      conf,
      classOf[TemporalGeoTiffInputFormat],
      classOf[TemporalProjectedExtent],
      classOf[Tile]
    )
  }

  def hadoopMultibandGeoTiffRDD(path: String): RDD[(ProjectedExtent, MultibandTile)] =
    hadoopMultibandGeoTiffRDD(new Path(path), HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions)

  def hadoopMultibandGeoTiffRDD(path: String, tiffExtension: String): RDD[(ProjectedExtent, MultibandTile)] =
    hadoopMultibandGeoTiffRDD(new Path(path), Seq(tiffExtension))

  def hadoopMultibandGeoTiffRDD(path: String, tiffExtensions: Seq[String]): RDD[(ProjectedExtent, MultibandTile)] =
    hadoopMultibandGeoTiffRDD(new Path(path), tiffExtensions)

  def hadoopMultibandGeoTiffRDD(path: Path, tiffExtensions: Seq[String] = HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions, crs: Option[CRS] = None): RDD[(ProjectedExtent, MultibandTile)] = {
    val conf = sc.hadoopConfiguration.withInputDirectory(path, tiffExtensions)
    crs.foreach(GeoTiffInputFormat.setCrs(conf, _))
    sc.newAPIHadoopRDD(
      conf,
      classOf[MultibandGeoTiffInputFormat],
      classOf[ProjectedExtent],
      classOf[MultibandTile]
    )
  }

  def hadoopTemporalMultibandGeoTiffRDD(path: String): RDD[(TemporalProjectedExtent, MultibandTile)] =
    hadoopTemporalMultibandGeoTiffRDD(new Path(path), HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions)

  def hadoopTemporalMultibandGeoTiffRDD(path: String, tiffExtension: String): RDD[(TemporalProjectedExtent, MultibandTile)] =
    hadoopTemporalMultibandGeoTiffRDD(new Path(path), Seq(tiffExtension))

  def hadoopTemporalMultibandGeoTiffRDD(path: String, tiffExtensions: Seq[String]): RDD[(TemporalProjectedExtent, MultibandTile)] =
    hadoopTemporalMultibandGeoTiffRDD(new Path(path), tiffExtensions)

  def hadoopTemporalMultibandGeoTiffRDD(
    path: Path,
    tiffExtensions: Seq[String] = HadoopGeoTiffRDD.Options.DEFAULT.tiffExtensions,
    timeTag: String = TemporalGeoTiffInputFormat.GEOTIFF_TIME_TAG_DEFAULT,
    timeFormat: String = TemporalGeoTiffInputFormat.GEOTIFF_TIME_FORMAT_DEFAULT,
    crs: Option[CRS] = None
  ): RDD[(TemporalProjectedExtent, MultibandTile)] = {
    val conf = sc.hadoopConfiguration.withInputDirectory(path, tiffExtensions)
    TemporalGeoTiffInputFormat.setTimeTag(conf, timeTag)
    TemporalGeoTiffInputFormat.setTimeFormat(conf, timeFormat)
    crs.foreach(GeoTiffInputFormat.setCrs(conf, _))
    sc.newAPIHadoopRDD(
      conf,
      classOf[TemporalMultibandGeoTiffInputFormat],
      classOf[TemporalProjectedExtent],
      classOf[MultibandTile]
    )
  }

  def newJob: Job =
    Job.getInstance(sc.hadoopConfiguration)

  def newJob(name: String) =
    Job.getInstance(sc.hadoopConfiguration, name)
}
