package geotrellis.spark.io.file.geotiff

import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.raster.io.geotiff.{AutoHigherResolution, OverviewStrategy}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.spark.io.ThreadConfig
import geotrellis.spark.io.hadoop.geotiff._
import geotrellis.spark.io.file.cog.byteReader
import geotrellis.util.ByteReader

import com.typesafe.config.ConfigFactory
import java.net.URI

case class FileGeoTiffLayerReader[M[T] <: Traversable[T]](
  attributeStore: AttributeStore[M, GeoTiffMetadata],
  layoutScheme: ZoomedLayoutScheme,
  resampleMethod: ResampleMethod = NearestNeighbor,
  strategy: OverviewStrategy = AutoHigherResolution,
  defaultThreads: Int = FileGeoTiffLayerReader.defaultThreadCount
) extends GeoTiffLayerReader[M] {
  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri)
}

object FileGeoTiffLayerReader {
  val defaultThreadCount: Int = ConfigFactory.load().getThreads("geotrellis.file.threads.collection.read")
}
