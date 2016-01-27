package geotrellis.spark.etl.hadoop

import geotrellis.spark.io.hadoop._
import geotrellis.spark._
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class NetCdfHadoopInput extends HadoopInput[SpaceTimeInputKey, SpaceTimeKey] {
  val format = "netcdf"

  def source(props: Parameters)(implicit sc: SparkContext): RDD[(SpaceTimeInputKey, V)] = sc.netCdfRDD(new Path(props("path")))
}