package geotrellis.spark.io.hadoop.geotiff

import geotrellis.spark.io.hadoop.HdfsUtils

import org.apache.hadoop.conf.Configuration
import org.apache.commons.io.IOUtils
import org.apache.hadoop.fs.Path
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.net.URI

object HadoopIMGeoTiffAttributeStore {
  def apply(
    name: String,
    uri: URI,
    conf: Configuration = new Configuration()
  ): InMemoryGeoTiffAttributeStore =
    new InMemoryGeoTiffAttributeStore {
      lazy val metadataList = HadoopGeoTiffInput.list(name, uri, conf)
      def persist(uri: URI): Unit = {
        val str = metadataList.toJson.compactPrint
        HdfsUtils.write(new Path(uri), conf) { IOUtils.write(str, _, "UTF-8") }
      }
    }
}
