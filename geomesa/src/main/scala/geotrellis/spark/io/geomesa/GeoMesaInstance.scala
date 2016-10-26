package geotrellis.spark.io.geomesa

import geotrellis.spark.LayerId
import geotrellis.util.annotations.experimental

import com.typesafe.scalalogging.LazyLogging
import org.geotools.data.DataStoreFinder
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore

import scala.collection.JavaConverters._

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental class GeoMesaInstance(val conf: Map[String, String])
    extends Serializable with LazyLogging {
  logger.error("GeoMesa support is experimental")

  val SEP = "__.__"

  def layerIdString(layerId: LayerId): String = s"${layerId.name}${SEP}${layerId.zoom}"

  def dataStore = DataStoreFinder.getDataStore(conf.asJava)
  def accumuloDataStore = dataStore.asInstanceOf[AccumuloDataStore]
}

/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental object GeoMesaInstance {

  /** $experimental */
  @experimental def apply(tableName: String, instanceName: String, zookeepers: String, user: String, password: String, useMock: Boolean = false): GeoMesaInstance = {
    new GeoMesaInstance(Map(
      "instanceId" -> instanceName,
      "zookeepers" -> zookeepers,
      "user"       -> user,
      "password"   -> password,
      "tableName"  -> tableName,
      "useMock"    -> s"$useMock"
    ))
  }
}
