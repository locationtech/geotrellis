package geotrellis.spark.io.geomesa

import geotrellis.spark.LayerId

import org.geotools.data.DataStoreFinder
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore

import scala.collection.JavaConverters._

case class GeoMesaAttributeStore(instanceName: String, zookeepers: String, user: String, password: String, useMock: Boolean = false) extends Serializable {
  val whenField  = "when"
  val whereField = "where"

  val SEP = "__.__"

  def getConf(tableName: String) =
    Map(
      "instanceId" -> instanceName,
      "zookeepers" -> zookeepers,
      "user"       -> user,
      "password"   -> password,
      "tableName"  -> tableName,
      "useMock"    -> s"$useMock"
    )

  def layerIdString(layerId: LayerId): String = s"${layerId.name}${SEP}${layerId.zoom}"

  def getAccumuloDataStore(tableName: String) =
    DataStoreFinder.getDataStore(getConf(tableName).asJava).asInstanceOf[AccumuloDataStore]
}
