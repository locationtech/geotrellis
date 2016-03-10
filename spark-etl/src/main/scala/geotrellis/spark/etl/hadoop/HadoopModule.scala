package geotrellis.spark.etl.hadoop

import geotrellis.spark.etl.TypedModule

trait HadoopModule extends TypedModule {
  register(new GeoTiffHadoopInput)
  register(new GridKeyHadoopOutput)
  register(new TemporalGeoTiffHadoopInput)
  register(new TemporalMultibandGeoTiffHadoopInput)
  register(new GridTimeKeyHadoopOutput)
  register(new GridKeyRenderOutput)
  register(new GeoTiffSequenceHadoopInput)
  register(new MultibandGeoTiffHadoopInput)
  register(new MultibandGridKeyHadoopOutput)
  register(new MultibandGridTimeKeyHadoopOutput)
  register(new MultibandGeoTiffSequenceHadoopInput)
}

object HadoopModule extends HadoopModule
