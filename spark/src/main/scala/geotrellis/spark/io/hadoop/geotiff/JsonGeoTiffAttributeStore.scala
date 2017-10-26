package geotrellis.spark.io.hadoop.geotiff

import geotrellis.vector._
import java.net.URI

case class JsonGeoTiffAttributeStore(
  uri: URI,
  readData: URI => Seq[GeoTiffMetadata]
) extends CollectionAttributeStore[GeoTiffMetadata] {
  lazy val data: Seq[GeoTiffMetadata] = readData(uri)

  def query(layerName: Option[String] = None, extent: Option[ProjectedExtent] = None): Seq[GeoTiffMetadata] = {
    (layerName, extent) match {
      case (Some(name), Some(ProjectedExtent(ext, crs))) =>
        data.filter { md => md.name == name && md.extent.reproject(md.crs, crs).intersects(ext) }
      case (_, Some(ProjectedExtent(ext, crs))) =>
        data.filter { md => md.extent.reproject(md.crs, crs).intersects(ext) }
      case (Some(name), _) => data.filter { md => md.name == name }
      case _ => data
    }
  }
}
