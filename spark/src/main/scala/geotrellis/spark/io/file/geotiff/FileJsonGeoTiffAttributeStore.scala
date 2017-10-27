package geotrellis.spark.io.file.geotiff

import geotrellis.spark.io.hadoop.geotiff._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.net.URI
import java.io.PrintWriter

object FileJsonGeoTiffAttributeStore {
  def readDataAsTree(uri: URI): GeoTiffMetadataTree[GeoTiffMetadata] =
    GeoTiffMetadataTree.fromGeoTiffMetadataList(HadoopJsonGeoTiffAttributeStore.readData(uri, new Configuration))

  def apply(uri: URI): JsonGeoTiffAttributeStore =
    JsonGeoTiffAttributeStore(uri, readDataAsTree)

  def apply(path: Path, name: String, uri: URI): JsonGeoTiffAttributeStore = {
    val conf = new Configuration
    val data = HadoopGeoTiffInput.list(name, uri, conf)
    val attributeStore = JsonGeoTiffAttributeStore(path.toUri, readDataAsTree)
    val fs = path.getFileSystem(conf)

    if(fs.exists(path)) {
      attributeStore
    } else {
      val fdos = fs.create(path)
      val out = new PrintWriter(fdos)
      try {
        val s = data.toJson.prettyPrint
        out.println(s)
      } finally {
        out.close()
        fdos.close()
      }

      attributeStore
    }
  }
}
