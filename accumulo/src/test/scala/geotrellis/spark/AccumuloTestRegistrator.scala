package geotrellis.spark

import com.esotericsoftware.kryo.Kryo

import scala.util.Properties

class AccumuloTestRegistrator extends TestRegistrator {
  override def registerClasses(kryo: Kryo) {
    super.registerClasses(kryo)
    if (Properties.envOrNone("GEOTRELLIS_KRYO_REGREQ") != None) {
      kryo.register(classOf[org.apache.accumulo.core.client.impl.ConnectorImpl])
      kryo.register(classOf[org.apache.accumulo.core.client.mock.MockConnector])
      kryo.register(classOf[org.apache.accumulo.core.client.BatchWriterConfig])
      kryo.register(classOf[org.apache.accumulo.core.client.Durability])
      kryo.register(classOf[org.apache.accumulo.core.data.Key])
      kryo.register(classOf[org.apache.accumulo.core.data.Value])
    }
  }
}
