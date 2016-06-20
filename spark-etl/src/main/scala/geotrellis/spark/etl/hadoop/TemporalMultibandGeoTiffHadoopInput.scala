package geotrellis.spark.etl.hadoop

import geotrellis.raster.MultibandTile
import geotrellis.spark.ingest._
import geotrellis.spark.io.hadoop._
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.io.hadoop.formats.TemporalGeoTiffInputFormat

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

class TemporalMultibandGeoTiffHadoopInput extends HadoopInput[TemporalProjectedExtent, MultibandTile] {
  val format = "temporal-geotiff"
  def apply(job: EtlJob)(implicit sc: SparkContext): RDD[(TemporalProjectedExtent, MultibandTile)] =
    sc.hadoopTemporalMultibandGeoTiffRDD(
      path       = job.inputProps("path"),
      timeTag    = job.config.ingestOptions.keyIndexMethod.timeTag.getOrElse(TemporalGeoTiffInputFormat.GEOTIFF_TIME_TAG_DEFAULT),
      timeFormat = job.config.ingestOptions.keyIndexMethod.timeFormat.getOrElse(TemporalGeoTiffInputFormat.GEOTIFF_TIME_FORMAT_DEFAULT)
    )
}

