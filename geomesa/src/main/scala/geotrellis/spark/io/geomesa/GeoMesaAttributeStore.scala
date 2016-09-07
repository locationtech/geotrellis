package geotrellis.spark.io.geomesa

import geotrellis.spark.LayerId

import org.apache.accumulo.core.client.ClientConfiguration
import org.apache.accumulo.core.client.mock.MockInstance
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.accumulo.core.security.Authorizations
import org.apache.hadoop.mapreduce.Job
import org.geotools.data.DataStoreFinder
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore

import scala.collection.JavaConverters._

case class GeoMesaAttributeStore(instanceName: String, zookeepers: String, user: String, password: String, useMock: Boolean = false) extends Serializable {
  val whenField  = "when"
  val whereField = "where"

  val SEP = "__.__"

  def getConf(tableName: String, raw: Boolean = false) = {
    if(isMock && !raw) {
      val connector = new MockInstance("fake").getConnector(user, new PasswordToken(password))
      connector.securityOperations().changeUserAuthorizations(user, new Authorizations("admin", "user"))

      Map(
        "connector" -> connector,
        "caching"   -> false,
        "tableName" -> tableName,
        "useMock"   -> s"$useMock"
      )
    } else
      Map(
        "instanceId" -> instanceName,
        "zookeepers" -> zookeepers,
        "user"       -> user,
        "password"   -> password,
        "tableName"  -> tableName,
        "useMock"    -> s"$useMock"
      )
  }

  def layerIdString(layerId: LayerId): String = s"${layerId.name}${SEP}${layerId.zoom}"

  def getAccumuloDataStore(tableName: String) =
    DataStoreFinder.getDataStore(getConf(tableName).asJava).asInstanceOf[AccumuloDataStore]

  def isMock = instanceName == "fake" && useMock
}
