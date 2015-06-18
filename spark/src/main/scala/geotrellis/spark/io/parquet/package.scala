package geotrellis.spark.io

package object parquet {

  implicit lazy val parquetSpatialRasterWriter = spatial.SpatialRasterRDDWriter

  implicit lazy val parquetSpaceTimeRasterWriter = spacetime.SpaceTimeRasterRDDWriter

}
