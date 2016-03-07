package geotrellis.spark.etl.hadoop

import geotrellis.spark.etl.TypedModule

trait HadoopModule extends TypedModule {
  register(new GeoTiffHadoopInput)
  register(new SpatialHadoopOutput)
  register(new TemporalGeoTiffHadoopInput)
  register(new TemporalMultibandGeoTiffHadoopInput)
  register(new SpaceTimeHadoopOutput)
  register(new SpatialRenderOutput)
  register(new GeoTiffSequenceHadoopInput)
  register(new MultibandGeoTiffHadoopInput)
  register(new MultibandSpatialHadoopOutput)
  register(new MultibandSpaceTimeHadoopOutput)
  register(new MultibandGeoTiffSequenceHadoopInput)
}

object HadoopModule extends HadoopModule
