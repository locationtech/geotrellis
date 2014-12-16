package geotrellis.spark.io.hadoop

import geotrellis.spark.ingest.ProjectedExtent
import geotrellis.spark.io.hadoop.formats._
import geotrellis.raster._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.hadoop.fs.Path

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

  def netCdfRDD(
    path: Path,
    inputFormat: NetCdfInputFormat = DefaultNetCdfInputFormat): RDD[(NetCdfBand, Tile)] = {
    val makeTime = (info: GdalRasterInfo) => {
      val baseString = info.file.meta(inputFormat.baseDateMetaDataKey)
      val (typ, base) = NetCdfInputFormat.readTypeAndDate(baseString)
      val v = info.bandMeta("NETCDF_DIM_Time").toDouble
      NetCdfInputFormat.incrementDate(typ, v, base)
    }

    gdalRDD(path)
      .map { case (info, tile) =>
      val band = NetCdfBand( //TODO: Remove varname
        extent = info.file.rasterExtent.extent,
        crs = info.file.crs,
        varName = info.bandMeta("NETCDF_VARNAME"),
        time = makeTime(info)
      )
      band -> tile
    }
  }
}
