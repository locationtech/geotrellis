package geotrellis.spark

import com.esotericsoftware.kryo.Kryo

import scala.util.Properties

class CassandraTestRegistrator extends TestRegistrator {
  override def registerClasses(kryo: Kryo) {
    super.registerClasses(kryo)
    if (Properties.envOrNone("GEOTRELLIS_KRYO_REGREQ") != None) {
      kryo.register(classOf[LayerId])
    }
  }
}
