package geotrellis.spark.io.accumulo

import geotrellis.spark.io._
import org.apache.accumulo.core.client.security.tokens.PasswordToken

class AccumuloAttributeStoreSpec extends AttributeStoreSpec {
  val accumulo = AccumuloInstance(
    instanceName = "fake",
    zookeeper = "localhost",
    user = "root",
    token = new PasswordToken("")
  )

  lazy val attributeStore = new AccumuloAttributeStore(accumulo.connector, "attributes")
}
