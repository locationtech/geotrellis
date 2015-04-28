package geotrellis.spark.io

package object cassandra {
  implicit lazy val cassandraSpatialRasterRDDReader = spatial.SpatialRasterRDDReader
  implicit lazy val cassandraSpatialTileReader = spatial.SpatialTileReader
  implicit lazy val cassandraSpatialRasterRDDWriter = spatial.SpatialRasterRDDWriter

  implicit lazy val cassandraSpaceTimeRasterRDDReader = spacetime.SpaceTimeRasterRDDReader
  implicit lazy val cassandraSpaceTimeTileReader = spacetime.SpaceTimeTileReader
  implicit lazy val cassandraSpaceTimeRasterRDDWriter = spacetime.SpaceTimeRasterRDDWriter
}
