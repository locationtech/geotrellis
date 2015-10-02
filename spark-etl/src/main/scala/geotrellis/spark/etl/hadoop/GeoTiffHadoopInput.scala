package geotrellis.spark.etl.hadoop

import geotrellis.spark._
import geotrellis.vector.ProjectedExtent
import geotrellis.spark.ingest._
import org.apache.spark.SparkContext
import geotrellis.spark.io.hadoop._
import org.apache.spark.rdd.RDD

class GeoTiffHadoopInput extends HadoopInput[ProjectedExtent, SpatialKey] {
  val format = "geotiff"
  def source(props: Parameters)(implicit sc: SparkContext): RDD[(ProjectedExtent, V)] = sc.hadoopGeoTiffRDD(props("path"))
}

