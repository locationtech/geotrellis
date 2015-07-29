package geotrellis.spark.io.hadoop

import geotrellis.spark.io.hadoop.formats._
import geotrellis.raster._
import geotrellis.vector._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Job

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

  def hadoopGeoTiffRDD(path: Path, tiffExtensions: Seq[String]): RDD[(ProjectedExtent, Tile)] = {
    val searchPath = path.toString match {
      case p if tiffExtensions.exists(p.endsWith) => path
      case p => new Path(p + "/*" + tiffExtensions.mkString("{", ",", "}"))
    }

    val updatedConf =
      sc.hadoopConfiguration.withInputDirectory(searchPath)

    sc.newAPIHadoopRDD(
      updatedConf,
      classOf[GeotiffInputFormat],
      classOf[ProjectedExtent],
      classOf[Tile]
    )
  }

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
    inputFormat: NetCdfInputFormat = DefaultNetCdfInputFormat): RDD[(NetCdfBand, Tile)] = {
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
        val band = NetCdfBand(
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
