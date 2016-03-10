package geotrellis.spark.etl.hadoop

import geotrellis.raster.MultibandTile
import geotrellis.vector.ProjectedExtent
import geotrellis.spark.io.hadoop._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class MultibandGeoTiffHadoopInput extends HadoopInput[ProjectedExtent, MultibandTile] {
  val format = "multiband-geotiff"
  def apply(props: Parameters)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    sc.hadoopMultibandGeoTiffRDD(props("path"))
}
