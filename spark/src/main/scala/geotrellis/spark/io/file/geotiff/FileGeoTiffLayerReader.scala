package geotrellis.spark.io.file.geotiff

import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.SinglebandGeoTiff
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.util.Filesystem
import geotrellis.spark.io.hadoop.geotiff._
import java.net.URI


/** Approach with TiffTags stored in a DB */
case class FileGeoTiffLayerReader[M[T] <: Traversable[T]](
  /** This should be done in a separate interface */
  attributeStore: AttributeStore[M, GeoTiffMetadata],
  layoutScheme: ZoomedLayoutScheme
) extends GeoTiffLayerReader[M] {
  protected def readSingleband(uri: URI): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(Filesystem.toMappedByteBuffer(uri.toString), false, true)
}
