package geotrellis.spark.io.hadoop.geotiff

import java.net.URI

import geotrellis.vector._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.io.Source

case class InMemoryGeoTiffAttributeStore(
  name: String,
  uri: URI,
  conf: Configuration = new Configuration()
) extends CollectionAttributeStore[GeoTiffMetadata] {
  lazy val data =
    HadoopGeoTiffInput2.seq(name, uri, conf)

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
