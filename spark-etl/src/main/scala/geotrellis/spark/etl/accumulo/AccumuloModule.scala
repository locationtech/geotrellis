package geotrellis.spark.etl.accumulo

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import geotrellis.spark.SpatialKey
import geotrellis.spark.etl.hadoop.HadoopModule._
import geotrellis.spark.etl.hadoop.{SpatialHadoopSink, GeoTiffHadoopIngest}
import geotrellis.spark.etl.{SinkPlugin, IngestPlugin}

object AccumuloModule extends AbstractModule {
  override def configure() {
    val sinkBinder = Multibinder.newSetBinder(binder(), classOf[SinkPlugin])
    sinkBinder.addBinding().to(classOf[SpatialAccumuloSink])
  }
}