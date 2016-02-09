package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark.io.hadoop._
import geotrellis.spark._
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class NetCdfHadoopInput extends HadoopInput[TemporalProjectedExtent, Tile] {
  val format = "netcdf"
  def apply(props: Parameters)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] = sc.netCdfRDD(new Path(props("path")))
}