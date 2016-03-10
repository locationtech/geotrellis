package geotrellis.spark.etl.s3

import geotrellis.spark.etl.TypedModule

object S3Module extends TypedModule {
  register(new GeoTiffS3Input)
  register(new TemporalGeoTiffS3Input)
  register(new GridKeyS3Output)
  register(new GridTimeKeyS3Output)
  register(new MultibandGeoTiffS3Input)
  register(new TemporalMultibandGeoTiffS3Input)
  register(new GridTimeKeyMultibandS3Output)
  register(new GridKeyMultibandS3Output)
}
