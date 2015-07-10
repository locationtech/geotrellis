package geotrellis.spark.etl.hadoop

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import geotrellis.spark.etl.{SinkPlugin, IngestPlugin}

object HadoopModule extends AbstractModule {
  override def configure() {
    val ingestBinder = Multibinder.newSetBinder(binder(), classOf[IngestPlugin])
    ingestBinder.addBinding().to(classOf[GeoTiffHadoopIngest])
    ingestBinder.addBinding().to(classOf[NetCdfHadoopIngest])

    val sinkBinder = Multibinder.newSetBinder(binder(), classOf[SinkPlugin])
    sinkBinder.addBinding().to(classOf[SpatialHadoopSink])
    sinkBinder.addBinding().to(classOf[SpaceTimeHadoopSink])
  }
}