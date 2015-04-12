package geotrellis.spark.io

package object cassandra {
  implicit lazy val cassandraSpatialRasterRDDReaderProvider = spatial.SpatialRasterRDDReaderProvider
  implicit lazy val cassandraSpatialTileReaderProvider = spatial.SpatialTileReaderProvider
  implicit lazy val cassandraSpatialRasterRDDWriterProvider = spatial.SpatialRasterRDDWriterProvider

/*
  implicit lazy val cassandraSpaceTimeRasterRDDReaderProvider = spacetime.SpaceTimeRasterRDDReaderProvider
  implicit lazy val cassandraSpaceTimeTileReaderProvider = spacetime.SpaceTimeTileReaderProvider
  implicit lazy val cassandraSpaceTimeRasterRDDWriterProvider = spacetime.SpaceTimeRasterRDDWriterProvider*/
}
