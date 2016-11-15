package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark.io.hadoop._
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class TemporalGeoTiffHadoopInput extends HadoopInput[TemporalProjectedExtent, Tile] {
  val format = "temporal-geotiff"
  def apply(conf: EtlConf)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, Tile)] =
    HadoopGeoTiffRDD.temporal(
      getPath(conf.input.backend).path,
      HadoopGeoTiffRDD.Options(
        timeTag    = conf.output.keyIndexMethod.timeTag.getOrElse(HadoopGeoTiffRDD.GEOTIFF_TIME_TAG_DEFAULT),
        timeFormat = conf.output.keyIndexMethod.timeFormat.getOrElse(HadoopGeoTiffRDD.GEOTIFF_TIME_FORMAT_DEFAULT),
        crs = conf.input.getCrs
      )
    )
}
