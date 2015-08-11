package geotrellis.spark.etl.hadoop

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import geotrellis.spark.etl.{OutputPlugin, InputPlugin}

object HadoopModule extends AbstractModule {
  override def configure() {
    val inputBinder = Multibinder.newSetBinder(binder(), classOf[InputPlugin])
    inputBinder.addBinding().to(classOf[GeoTiffHadoopInput])
    inputBinder.addBinding().to(classOf[NetCdfHadoopInput])

    val outputBinder = Multibinder.newSetBinder(binder(), classOf[OutputPlugin])
    outputBinder.addBinding().to(classOf[SpatialHadoopOutput])
    outputBinder.addBinding().to(classOf[SpaceTimeHadoopOutput])
  }
}