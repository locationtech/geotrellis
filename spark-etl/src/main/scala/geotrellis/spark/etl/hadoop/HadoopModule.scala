package geotrellis.spark.etl.hadoop

import geotrellis.spark.etl.TypedModule

object HadoopModule extends TypedModule {
  register(new GeoTiffHadoopInput)
  register(new NetCdfHadoopInput)
  register(new SpatialHadoopOutput)
  register(new SpaceTimeHadoopOutput)
  register(new SpatialRenderOutput)
}
