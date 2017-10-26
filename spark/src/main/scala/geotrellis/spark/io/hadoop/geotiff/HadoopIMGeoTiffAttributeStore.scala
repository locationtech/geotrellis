package geotrellis.spark.io.hadoop.geotiff

import org.apache.hadoop.conf.Configuration
import java.net.URI

object HadoopIMGeoTiffAttributeStore {
  def apply(
    name: String,
    uri: URI,
    conf: Configuration = new Configuration()
  ): InMemoryGeoTiffAttributeStore =
    InMemoryGeoTiffAttributeStore(() => HadoopGeoTiffInput.list(name, uri, conf))
}
