package geotrellis.spark.io

import org.apache.spark.rdd.RDD

package object s3 {
  private[s3]
  def makePath(chunks: String*) =
    chunks.filter(_.nonEmpty).mkString("/")

  implicit def spatialSinglebandGeoTiffS3InputFormattable = SpatialSinglebandGeoTiffS3InputFormattable
  implicit def spatialMultibandGeoTiffS3InputFormattable = SpatialMultibandGeoTiffS3InputFormattable
  implicit def temporalSinglebandGeoTiffS3InputFormattable = TemporalSinglebandGeoTiffS3InputFormattable
  implicit def temporalMultibandGeoTiffS3InputFormattable = TemporalMultibandGeoTiffS3InputFormattable

  implicit class withSaveToS3Methods[K](rdd: RDD[(K, Array[Byte])]) extends SaveToS3Methods(rdd)
}
