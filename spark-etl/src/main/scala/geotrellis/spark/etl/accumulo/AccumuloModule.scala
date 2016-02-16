package geotrellis.spark.etl.accumulo

import geotrellis.spark.etl.TypedModule

object AccumuloModule extends TypedModule {
  register(new SpatialAccumuloOutput)
  register(new SpaceTimeAccumuloOutput)
  register(new MultibandSpatialAccumuloOutput)
  register(new MultibandSpaceTimeAccumuloOutput)
}
