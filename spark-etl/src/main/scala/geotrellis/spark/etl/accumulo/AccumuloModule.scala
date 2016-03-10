package geotrellis.spark.etl.accumulo

import geotrellis.spark.etl.TypedModule

object AccumuloModule extends TypedModule {
  register(new GridKeyAccumuloOutput)
  register(new GridTimeKeyAccumuloOutput)
  register(new MultibandGridKeyAccumuloOutput)
  register(new MultibandGridTimeKeyAccumuloOutput)
}
