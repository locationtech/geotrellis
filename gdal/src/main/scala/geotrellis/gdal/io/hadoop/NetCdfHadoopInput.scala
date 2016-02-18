package geotrellis.gdal.io.hadoop

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.etl.hadoop.HadoopInput
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD


class NetCdfHadoopInput extends HadoopInput[TemporalProjectedExtent, Tile] {
  val format = "netcdf"
  def apply(props: Parameters)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    sc.netCdfRDD(new Path(props("path")))
}
