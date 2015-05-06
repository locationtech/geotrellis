package geotrellis.spark.io

import geotrellis.spark.{SpaceTimeKey, SpatialKey}

package object s3 {
  implicit lazy val s3SpatialRasterRDDReader = spatial.SpatialRasterRDDReader
  implicit lazy val s3SpatialRasterRDDWriter = spatial.SpatialRasterRDDWriter
  implicit lazy val s3SpatialRasterTileReader = spatial.SpatialTileReader

  implicit lazy val s3SpaceTimeRasterRDDReader = spacetime.SpaceTimeRasterRDDReader
  implicit lazy val s3SpaceTimeRasterRDDWriter = spacetime.SpaceTimeRasterRDDWriter
  implicit lazy val s3SpaceTimeRasterTileReader = spacetime.SpaceTimeTileReader
}