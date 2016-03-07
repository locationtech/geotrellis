package geotrellis.spark.etl.hadoop

import geotrellis.raster.MultiBandTile
import geotrellis.spark.ingest._
import geotrellis.spark.io.hadoop._
import geotrellis.spark._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class TemporalMultibandGeoTiffHadoopInput extends HadoopInput[TemporalProjectedExtent, MultiBandTile] {
  val format = "temporal-geotiff"
  def apply(props: Parameters)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultiBandTile)] = sc.hadoopTemporalMultiBandGeoTiffRDD(props("path"))
}

