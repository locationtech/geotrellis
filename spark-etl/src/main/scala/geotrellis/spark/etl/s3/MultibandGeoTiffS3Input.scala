package geotrellis.spark.etl.s3

import geotrellis.raster.MultibandTile
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.io.s3.MultibandGeoTiffS3InputFormat
import geotrellis.vector.ProjectedExtent
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD


class MultibandGeoTiffS3Input extends S3Input[ProjectedExtent, MultibandTile] {
  val format = "multiband-geotiff"
  def apply(job: EtlJob)(implicit sc: SparkContext): RDD[(ProjectedExtent, MultibandTile)] =
    sc.newAPIHadoopRDD(
      configuration(job.inputProps), classOf[MultibandGeoTiffS3InputFormat], classOf[ProjectedExtent], classOf[MultibandTile]
    )
}
