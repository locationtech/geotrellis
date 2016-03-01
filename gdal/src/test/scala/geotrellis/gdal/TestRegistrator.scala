package geotrellis.gdal

import geotrellis.spark.io.kryo.{ KryoRegistrator => NormalKryoRegistrator }

import com.esotericsoftware.kryo.Kryo

import scala.util.Properties


class TestRegistrator extends NormalKryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    super.registerClasses(kryo)
    if (Properties.envOrNone("GEOTRELLIS_KRYO_REGREQ") != None) {
      kryo.setRegistrationRequired(true)
    }
  }
}
