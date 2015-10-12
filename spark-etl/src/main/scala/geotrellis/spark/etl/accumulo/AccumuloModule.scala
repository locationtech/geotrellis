package geotrellis.spark.etl.accumulo

import geotrellis.spark.etl.TypedModule

object AccumuloModule extends TypedModule {
  register(new SpatialAccumuloInput)
  register(new SpaceTimeAccumuloInput)
  register(new SpatialAccumuloOutput)
  register(new SpaceTimeAccumuloOutput)
}
