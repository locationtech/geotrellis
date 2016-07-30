package geotrellis.spark.etl.hbase

import geotrellis.spark.etl.TypedModule

object HBaseModule extends TypedModule {
  register(new SpatialHBaseOutput)
  register(new SpaceTimeHBaseOutput)
  register(new MultibandSpatialHBaseOutput)
  register(new MultibandSpaceTimeHBaseOutput)
}
