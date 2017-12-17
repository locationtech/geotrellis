package geotrellis.spark.io.file.geotiff

import geotrellis.spark.io.hadoop.geotiff._
import org.apache.hadoop.conf.Configuration

import java.net.URI

object FileIMGeoTiffAttributeStore {
  def apply(
    name: String,
    uri: URI
  ): InMemoryGeoTiffAttributeStore =
    InMemoryGeoTiffAttributeStore(() => GeoTiffMetadataTree.fromGeoTiffMetadataList(HadoopGeoTiffInput.list(name, uri, new Configuration())))
}
