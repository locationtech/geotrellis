package geotrellis.spark.io.hadoop.geotiff

import geotrellis.vector.ProjectedExtent
import java.net.URI

case class InMemoryGeoTiffAttributeStore(getData: () => GeoTiffMetadataTree[GeoTiffMetadata]) extends CollectionAttributeStore[GeoTiffMetadata] {
  lazy val data = getData()
  def query(layerName: Option[String] = None, extent: Option[ProjectedExtent] = None): Seq[GeoTiffMetadata] = {
    (layerName, extent) match {
      case (Some(name), Some(projectedExtent)) =>
        data.query(projectedExtent).filter { md => md.name == name }
      case (_, Some(projectedExtent)) =>
        data.query(projectedExtent)
      case (Some(name), _) => data.query().filter { md => md.name == name }
      case _ => data.query()
    }
  }

  def persist(uri: URI): Unit = { }
}
