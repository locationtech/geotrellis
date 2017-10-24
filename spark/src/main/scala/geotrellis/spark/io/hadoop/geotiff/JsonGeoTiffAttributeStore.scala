package geotrellis.spark.io.hadoop.geotiff

import geotrellis.vector._


import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.net.URI
import scala.io.Source

case class JsonGeoTiffAttributeStore(
  uri: URI,
  conf: Configuration = new Configuration()
) extends CollectionAttributeStore[GeoTiffMetadata] {
  lazy val data = {
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

  def query(layerName: Option[String] = None, extent: Option[ProjectedExtent] = None): Seq[GeoTiffMetadata] = {
    (layerName, extent) match {
      case (Some(name), Some(ProjectedExtent(ext, crs))) =>
        data.filter { md =>
          md.name == name && md.extent.reproject(md.crs, crs).intersects(ext)
        }
      case (_, Some(ProjectedExtent(ext, crs))) =>
        data.filter { md =>
          md.extent.reproject(md.crs, crs).intersects(ext)
        }
      case (Some(name), _) =>
        data.filter { md =>
          md.name == name
        }
      case _ => data
    }
  }
}
