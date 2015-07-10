package geotrellis.spark.etl.s3

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import geotrellis.spark.SpatialKey
import geotrellis.spark.etl.{SinkPlugin, IngestPlugin}


object S3Module extends AbstractModule {

  override def configure() {
    val ingestBinder = Multibinder.newSetBinder(binder(), classOf[IngestPlugin])
    ingestBinder.addBinding().to(classOf[GeoTiffS3Ingest])
    ingestBinder.addBinding().to(classOf[TemporalGeoTiffS3Ingest])

    val sinkBinder = Multibinder.newSetBinder(binder(), classOf[SinkPlugin])
    sinkBinder.addBinding().to(classOf[SpatialS3Sink])
    sinkBinder.addBinding().to(classOf[SpaceTimeS3Sink])
  }
}