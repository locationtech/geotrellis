package geotrellis.spark.io.hadoop.geotiff

import geotrellis.proj4.CRS
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.spark._
import geotrellis.spark.io._

import spray.json._
import spray.json.DefaultJsonProtocol._

import java.net.URI

// Row in a table
case class GeoTiffMetadata(
  extent: Extent,
  crs: CRS,
  name: String, // name of the ingest data set, by default each tile a separate layer
  uri: URI
) {
  def projectedExtent: ProjectedExtent = ProjectedExtent(extent, crs)
}

object GeoTiffMetadata {
  implicit val geoTiffMetadataFormat  = jsonFormat4(GeoTiffMetadata.apply)
}

case class TemporalGeoTiffMetadata(
  extent: Extent,
  time: Long,
  crs: CRS,
  name: String,
  uri: URI
) {
  def projectedExtent: ProjectedExtent = ProjectedExtent(extent, crs)
  def temporalProjectedExtent: TemporalProjectedExtent = TemporalProjectedExtent(extent, crs, time)
}

object TemporalGeoTiffMetadata {
  implicit val temporalGeoTiffMetadataFormat  = jsonFormat5(TemporalGeoTiffMetadata.apply)
}