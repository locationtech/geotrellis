package geotrellis.spark.io.file.geotiff

import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.{AutoHigherResolution, OverviewStrategy, SinglebandGeoTiff}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.io.ThreadConfig
import geotrellis.spark.io.hadoop.geotiff._
import geotrellis.util.Filesystem

import com.typesafe.config.ConfigFactory

import java.net.URI

/** Approach with TiffTags stored in a DB */
case class FileGeoTiffLayerReader[M[T] <: Traversable[T]](
  /** This should be done in a separate interface */
  attributeStore: AttributeStore[M, GeoTiffMetadata],
  layoutScheme: ZoomedLayoutScheme,
  resampleMethod: ResampleMethod = NearestNeighbor,
  strategy: OverviewStrategy = AutoHigherResolution,
  defaultThreads: Int = FileGeoTiffLayerReader.defaultThreadCount
) extends GeoTiffLayerReader[M] {
  protected def readSingleband(uri: URI): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(Filesystem.toMappedByteBuffer(uri.toString), false, true)
}

object FileGeoTiffLayerReader {
  val defaultThreadCount: Int = ConfigFactory.load().getThreads("geotrellis.file.threads.collection.read")
}
