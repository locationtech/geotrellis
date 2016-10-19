package geotrellis.spark.io.geomesa

import geotrellis.spark.LayerId

import org.geotools.data.DataStoreFinder
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore

import scala.collection.JavaConverters._

class GeoMesaInstance(val conf: Map[String, String]) extends Serializable {
  val SEP = "__.__"

  def layerIdString(layerId: LayerId): String = s"${layerId.name}${SEP}${layerId.zoom}"

  def dataStore = DataStoreFinder.getDataStore(conf.asJava)
  def accumuloDataStore = dataStore.asInstanceOf[AccumuloDataStore]
}

object GeoMesaInstance {
  def apply(tableName: String, instanceName: String, zookeepers: String, user: String, password: String, useMock: Boolean = false): GeoMesaInstance = {
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
