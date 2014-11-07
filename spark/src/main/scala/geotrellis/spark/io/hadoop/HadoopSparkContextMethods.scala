package geotrellis.spark.io.hadoop

import geotrellis.spark.ingest.ProjectedExtent
import geotrellis.spark.io.hadoop.formats._
import geotrellis.raster._
import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.hadoop.fs.Path
import org.joda.time.{DateTimeZone, DateTime}

trait HadoopSparkContextMethods {
  val sc: SparkContext

  def hadoopGeoTiffRDD(path: String): RDD[(ProjectedExtent, Tile)] =
    hadoopGeoTiffRDD(new Path(path))

  def hadoopGeoTiffRDD(path: Path): RDD[(ProjectedExtent, Tile)] = {
    val updatedConf =
      sc.hadoopConfiguration.withInputDirectory(path)

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

  def netCdfRDD(path: Path): RDD[(NetCdfBand, Tile)] = {
    val makeTime = (info: GdalRasterInfo) => {
      require(info.file.meta("Time#units") == "days since 1950-01-01")
      val base = new DateTime(1950, 1, 1, 0, 0, 0, DateTimeZone.UTC)
      val days = info.bandMeta("NETCDF_DIM_Time").toDouble
      base.plusDays(days.toInt).plusHours((days % 1 * 24).toInt)
    }

    gdalRDD(path)
      .map { case (info, tile) =>
      val band = NetCdfBand(
        extent = info.file.rasterExtent.extent,
        crs = info.file.crs,
        varName = info.bandMeta("NETCDF_VARNAME"),
        time = makeTime(info)
      )
      band -> tile
    }
  }
}
