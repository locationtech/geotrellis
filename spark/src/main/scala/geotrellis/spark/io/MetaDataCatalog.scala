package geotrellis.spark.io

import geotrellis.spark._

import org.apache.accumulo.core.client.Connector
import org.apache.accumulo.core.data.{Key, Value, Mutation}
import org.apache.accumulo.core.security.Authorizations
import org.apache.hadoop.io.Text
import org.apache.spark.Logging
import spray.json.RootJsonFormat

import scala.util.Try

trait MetaDataCatalog[Params] {
  /** Return [[geotrellis.spark.RasterMetaData]] and matching Params if unique match is found based on [[LayerId]] */
  def load(layerId: LayerId): Try[(LayerMetaData, Params)]

  /** Return [[geotrellis.spark.RasterMetaData]] matching both LayerId and params. */
  def load(layerId: LayerId, params: Params): Try[LayerMetaData]

  /** Save [[geotrellis.spark.RasterMetaData]] such that it can be found by the load functions */
  def save(layerId: LayerId, params: Params,  metaData: LayerMetaData, clobber: Boolean): Try[Unit]
}
