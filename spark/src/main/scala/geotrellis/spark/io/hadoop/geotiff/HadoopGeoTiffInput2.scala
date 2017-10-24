package geotrellis.spark.io.hadoop.geotiff

import java.io.PrintWriter

import geotrellis.spark.io.hadoop.{HdfsRangeReader, HdfsUtils}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.rdd.RDD
import java.net.URI
import java.nio.file.{Files, Paths}

import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.util.Filesystem
import geotrellis.vector.ProjectedExtent
import spray.json._
import spray.json.DefaultJsonProtocol._

object HadoopGeoTiffInput2 {
  /**
    *
    * Returns a list of URIs matching given regexp
    *
    * @name - group name
    * @uri - regexp
    * @conf - hadoopConfiguration
    */
  def seq(name: String, uri: URI, conf: Configuration): List[GeoTiffMetadata] = {
    HdfsUtils
      .listFiles(new Path(uri), conf)
      .map { p =>
        val tiffTags = TiffTagsReader.read(HdfsRangeReader(p, conf))
        GeoTiffMetadata(tiffTags.extent, tiffTags.crs, name, uri)
      }
  }

  def jsonGeoTiffAttributeStore(path: Path, name: String, uri: URI, conf: Configuration): JsonGeoTiffAttributeStore = {
    val data = seq(name, uri, conf)
    val attributeStore = JsonGeoTiffAttributeStore(path.toUri, conf)
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
