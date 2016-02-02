package geotrellis.spark.io.hadoop

import geotrellis.raster._
import geotrellis.spark.TemporalProjectedExtent
import geotrellis.vector._
import geotrellis.spark.io.hadoop.formats._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.conf.Configuration

trait HadoopSparkContextMethods {
  val sc: SparkContext
  val defaultTiffExtensions: Seq[String] = Seq(".tif", ".TIF", ".tiff", ".TIFF")

  def hadoopGeoTiffRDD(path: String): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(new Path(path), defaultTiffExtensions)

  def hadoopGeoTiffRDD(path: String, tiffExtension: String): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(new Path(path), Seq(tiffExtension))

  def hadoopGeoTiffRDD(path: String, tiffExtensions: Seq[String] ): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(new Path(path), tiffExtensions)

  def hadoopGeoTiffRDD(path: Path): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(path, defaultTiffExtensions)

  def hadoopGeoTiffRDD(path: Path, tiffExtension: String): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(path, Seq(tiffExtension))

  def hadoopGeoTiffRDD(path: Path, tiffExtensions: Seq[String]): RDD[(ProjectedExtent, Tile)] =
    sc.newAPIHadoopRDD(
      sc.hadoopConfiguration.withInputDirectory(path, tiffExtensions),
      classOf[GeotiffInputFormat],
      classOf[ProjectedExtent],
      classOf[Tile]
    )

  def hadoopSpaceTimeGeoTiffRDD(path: String): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopSpaceTimeGeoTiffRDD(new Path(path), defaultTiffExtensions)

  def hadoopSpaceTimeGeoTiffRDD(path: String, tiffExtension: String): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopSpaceTimeGeoTiffRDD(new Path(path), Seq(tiffExtension))

  def hadoopSpaceTimeGeoTiffRDD(path: String, tiffExtensions: Seq[String] ): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopSpaceTimeGeoTiffRDD(new Path(path), tiffExtensions)

  def hadoopSpaceTimeGeoTiffRDD(path: Path): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopSpaceTimeGeoTiffRDD(path, defaultTiffExtensions)

  def hadoopSpaceTimeGeoTiffRDD(path: Path, tiffExtension: String): RDD[(TemporalProjectedExtent, Tile)] =
    hadoopSpaceTimeGeoTiffRDD(path, Seq(tiffExtension))

  def hadoopSpaceTimeGeoTiffRDD(path: Path, tiffExtensions: Seq[String]): RDD[(TemporalProjectedExtent, Tile)] =
    sc.newAPIHadoopRDD(
      sc.hadoopConfiguration.withInputDirectory(path, tiffExtensions),
      classOf[SpaceTimeGeoTiffInputFormat],
      classOf[TemporalProjectedExtent],
      classOf[Tile]
    )

  def hadoopMultiBandGeoTiffRDD(path: String): RDD[(ProjectedExtent, MultiBandTile)] =
    hadoopMultiBandGeoTiffRDD(new Path(path), defaultTiffExtensions)

  def hadoopMultiBandGeoTiffRDD(path: String, tiffExtension: String): RDD[(ProjectedExtent, MultiBandTile)] =
    hadoopMultiBandGeoTiffRDD(new Path(path), Seq(tiffExtension))

  def hadoopMultiBandGeoTiffRDD(path: String, tiffExtensions: Seq[String]): RDD[(ProjectedExtent, MultiBandTile)] =
    hadoopMultiBandGeoTiffRDD(new Path(path), tiffExtensions)

  def hadoopMultiBandGeoTiffRDD(path: Path, tiffExtensions: Seq[String] = defaultTiffExtensions): RDD[(ProjectedExtent, MultiBandTile)] =
    sc.newAPIHadoopRDD(
      sc.hadoopConfiguration.withInputDirectory(path, tiffExtensions),
      classOf[MultiBandGeoTiffInputFormat],
      classOf[ProjectedExtent],
      classOf[MultiBandTile]
    )

  def gdalRDD(path: Path): RDD[(GdalRasterInfo, Tile)] = {
    val updatedConf = sc.hadoopConfiguration.withInputDirectory(path)

    sc.newAPIHadoopRDD(
      updatedConf,
      classOf[GdalInputFormat],
      classOf[GdalRasterInfo],
      classOf[Tile]
    )
  }

  def netCdfRDD(
    path: Path,
    inputFormat: NetCdfInputFormat = DefaultNetCdfInputFormat): RDD[(TemporalProjectedExtent, Tile)] = {
    val makeTime = (info: GdalRasterInfo) =>
    info.file.meta.find {
      case(key, value) => key.toLowerCase == inputFormat.baseDateMetaDataKey.toLowerCase
    }.map(_._2) match {
      case Some(baseString) => {

        val (typ, base) = NetCdfInputFormat.readTypeAndDate(
          baseString,
          inputFormat.dateTimeFormat,
          inputFormat.yearOffset,
          inputFormat.monthOffset,
          inputFormat.dayOffset
        )

        info.bandMeta.find {
          case(key, value) => key.toLowerCase == "netcdf_dim_time"
        }.map(_._2) match {
          case Some(s) => NetCdfInputFormat.incrementDate(typ, s.toDouble, base)
          case _ => base
        }
      }
      case None => throw new IllegalArgumentException("Can't find base date!")
    }

    gdalRDD(path)
      .map { case (info, tile) =>
        val band = TemporalProjectedExtent(
          extent = info.file.rasterExtent.extent,
          crs = info.file.crs,
          time = makeTime(info)
        )
        band -> tile
    }
  }

  def newJob: Job =
    Job.getInstance(sc.hadoopConfiguration)

  def newJob(name: String) =
    Job.getInstance(sc.hadoopConfiguration, name)
}
