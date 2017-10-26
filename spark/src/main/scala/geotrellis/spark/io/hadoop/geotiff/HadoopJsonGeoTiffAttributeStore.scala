package geotrellis.spark.io.hadoop.geotiff

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.net.URI
import java.io.PrintWriter

import scala.io.Source

object HadoopJsonGeoTiffAttributeStore {
  def readData(uri: URI, conf: Configuration): Seq[GeoTiffMetadata] = {
    val path = new Path(uri)
    val fs = path.getFileSystem(conf)
    val stream = fs.open(path)
    val json = try {
      Source
        .fromInputStream(stream)
        .getLines
        .mkString(" ")
    } finally stream.close()

    json
      .parseJson
      .convertTo[Seq[GeoTiffMetadata]]
  }


  def apply(uri: URI): JsonGeoTiffAttributeStore =
    JsonGeoTiffAttributeStore(uri, readData(_, new Configuration))

  def apply(path: Path, name: String, uri: URI, conf: Configuration): JsonGeoTiffAttributeStore = {
    val data = HadoopGeoTiffInput.list(name, uri, conf)
    val attributeStore = JsonGeoTiffAttributeStore(path.toUri, readData(_, conf))
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
