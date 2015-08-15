package geotrellis.spark.etl.s3

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import geotrellis.spark.SpatialKey
import geotrellis.spark.etl.{OutputPlugin, InputPlugin}


object S3Module extends AbstractModule {

  override def configure() {
    val inputBinder = Multibinder.newSetBinder(binder(), classOf[InputPlugin])
    inputBinder.addBinding().to(classOf[GeoTiffS3Input])
    inputBinder.addBinding().to(classOf[TemporalGeoTiffS3Input])

    val outputBinder = Multibinder.newSetBinder(binder(), classOf[OutputPlugin])
    outputBinder.addBinding().to(classOf[SpatialS3Output])
    outputBinder.addBinding().to(classOf[SpaceTimeS3Output])
  }
}