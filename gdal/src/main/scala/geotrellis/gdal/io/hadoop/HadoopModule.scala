package geotrellis.gdal.io.hadoop

import geotrellis.spark.etl.hadoop._
import geotrellis.spark.etl.TypedModule
import geotrellis.spark.etl.hadoop.{ HadoopModule => EtlHadoopModule }

object HadoopModule extends EtlHadoopModule {
  register(new GeoTiffHadoopInput)
  register(new GeoTiffSequenceHadoopInput)
  register(new NetCdfHadoopInput)
  register(new SpaceTimeHadoopOutput)
  register(new SpatialHadoopOutput)
  register(new SpatialRenderOutput)
}
