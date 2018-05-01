package geotrellis.spark.io.file.geotiff

import geotrellis.spark.io.hadoop.geotiff._

import org.apache.hadoop.conf.Configuration
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.io.PrintWriter
import java.net.URI

object FileIMGeoTiffAttributeStore {
  def apply(
    name: String,
    uri: URI
  ): InMemoryGeoTiffAttributeStore = {
    def getDataFunction = () => HadoopGeoTiffInput.list(name, uri, new Configuration())
    new InMemoryGeoTiffAttributeStore(() => GeoTiffMetadataTree.fromGeoTiffMetadataList(getDataFunction())) {
      def persist(uri: URI): Unit = {
        val data = getDataFunction()
        val str = data.toJson.compactPrint
        new PrintWriter(uri.toString) { write(str); close }
      }
    }
  }
}
