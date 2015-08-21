package geotrellis.spark.etl.accumulo

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import geotrellis.spark.SpatialKey
import geotrellis.spark.etl.hadoop.HadoopModule._
import geotrellis.spark.etl.hadoop.{SpatialHadoopOutput, GeoTiffHadoopInput}
import geotrellis.spark.etl.{OutputPlugin, InputPlugin}

object AccumuloModule extends AbstractModule {
  override def configure() {
    val inputBinder = Multibinder.newSetBinder(binder(), classOf[InputPlugin])
    inputBinder.addBinding().to(classOf[SpatialAccumuloInput])
    inputBinder.addBinding().to(classOf[SpaceTimeAccumuloInput])

    val outputBinder = Multibinder.newSetBinder(binder(), classOf[OutputPlugin])
    outputBinder.addBinding().to(classOf[SpatialAccumuloOutput])
    outputBinder.addBinding().to(classOf[SpaceTimeAccumuloOutput])
  }
}