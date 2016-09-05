package geotrellis.spark.io.geomesa

import geotrellis.spark.LayerId
import org.apache.hadoop.io.Text
import org.geotools.data.DataStoreFinder
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore

import scala.collection.JavaConverters._

case class GeoMesaAttributeStore(instanceName: String, zookeepers: String, user: String, password: String) extends Serializable {
  val dsConf = Map(
    "instanceId" -> instanceName,
    "zookeepers" -> zookeepers,
    "user"       -> user,
    "password"   -> password
  )

  val valueField = "value"
  val whenField  = "when"
  val whereField = "where"

  val SEP = "__.__"

  def layerIdString(layerId: LayerId): String = s"${layerId.name}${SEP}${layerId.zoom}"

  def getAccumuloDataStore(tableName: String) =
    DataStoreFinder.getDataStore((dsConf + ("tableName" -> tableName)).asJava).asInstanceOf[AccumuloDataStore]

}
