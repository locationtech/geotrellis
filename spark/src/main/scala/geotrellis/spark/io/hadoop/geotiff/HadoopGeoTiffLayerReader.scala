package geotrellis.spark.io.hadoop.geotiff

import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.{AutoHigherResolution, OverviewStrategy, SinglebandGeoTiff}
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.util.StreamingByteReader
import geotrellis.spark.io.hadoop.HdfsRangeReader

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import java.net.URI

/** Approach with TiffTags stored in a DB */
case class HadoopGeoTiffLayerReader[M[T] <: Traversable[T]](
  /** This should be done in a separate interface */
  attributeStore: AttributeStore[M, GeoTiffMetadata],
  layoutScheme: ZoomedLayoutScheme,
  resampleMethod: ResampleMethod = NearestNeighbor,
  strategy: OverviewStrategy = AutoHigherResolution,
  conf: Configuration = new Configuration
) extends GeoTiffLayerReader[M] {
  protected def readSingleband(uri: URI): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(StreamingByteReader(HdfsRangeReader(new Path(uri), conf)), false, true)
}
