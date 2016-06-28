package geotrellis.spark.etl.file

import geotrellis.spark.etl.TypedModule

trait FileModule extends TypedModule {
  register(new SpatialFileOutput)
  register(new SpaceTimeFileOutput)
  register(new MultibandSpatialFileOutput)
  register(new MultibandSpaceTimeFileOutput)
}

object FileModule extends FileModule
