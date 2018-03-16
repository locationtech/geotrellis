package geotrellis.spark.io.hadoop.geotiff

import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.raster.io.geotiff.{AutoHigherResolution, OverviewStrategy}
import geotrellis.spark.io.hadoop.cog.byteReader
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.util.ByteReader
import geotrellis.spark.io._

import com.typesafe.config.ConfigFactory
import org.apache.hadoop.conf.Configuration

import java.net.URI

case class HadoopGeoTiffLayerReader[M[T] <: Traversable[T]](
  attributeStore: AttributeStore[M, GeoTiffMetadata],
  layoutScheme: ZoomedLayoutScheme,
  resampleMethod: ResampleMethod = NearestNeighbor,
  strategy: OverviewStrategy = AutoHigherResolution,
  conf: Configuration = new Configuration,
  defaultThreads: Int = HadoopGeoTiffLayerReader.defaultThreadCount
) extends GeoTiffLayerReader[M] {
  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri, conf)
}

object HadoopGeoTiffLayerReader {
  val defaultThreadCount: Int = ConfigFactory.load().getThreads("geotrellis.hadoop.threads.collection.read")
}
