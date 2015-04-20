package geotrellis.spark.io

import geotrellis.spark.{SpaceTimeKey, SpatialKey}

package object s3 {
  implicit lazy val s3SpatialRasterRDDReader = spatial.SpatialRasterRDDReader
  implicit lazy val s3SpatialRasterRDDWriter = new RasterRDDWriter[SpatialKey]

  implicit lazy val s3SpaceTimeRasterRDDReader = spacetime.SpaceTimeRasterRDDReader
  implicit lazy val s3SpaceTimeRasterRDDWriter = new RasterRDDWriter[SpaceTimeKey]
}