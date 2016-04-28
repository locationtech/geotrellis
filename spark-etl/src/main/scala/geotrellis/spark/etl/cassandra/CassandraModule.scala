package geotrellis.spark.etl.cassandra

import geotrellis.spark.etl.TypedModule

object CassandraModule extends TypedModule {
  register(new SpatialCassandraOutput)
  register(new SpaceTimeCassandraOutput)
  register(new MultibandSpatialCassandraOutput)
  register(new MultibandSpaceTimeCassandraOutput)
}
