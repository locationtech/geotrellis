package geotrellis.spark.io

import geotrellis.spark.{SpaceTimeKey, SpatialKey}

package object s3 {
  implicit lazy val s3SpatialRasterRDDReaderProvider = spatial.SpatialRasterRDDReaderProvider
  implicit lazy val s3SpatialRasterRDDWriterProvider = new RasterRDDWriterProvider[SpatialKey]

  implicit lazy val s3SpaceTimeRasterRDDReaderProvider = spacetime.SpaceTimeRasterRDDReaderProvider
  implicit lazy val s3SpaceTimeRasterRDDWriterProvider = new RasterRDDWriterProvider[SpaceTimeKey]
}