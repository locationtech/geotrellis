package geotrellis.spark

import geotrellis.spark.io.kryo.{ KryoRegistrator => NormalKryoRegistrator }

import org.apache.avro.Schema
import org.apache.avro.Schema.{Field, Type}
import com.esotericsoftware.kryo.Kryo

import scala.util.Properties

class TestRegistrator extends NormalKryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    super.registerClasses(kryo)
    if (Properties.envOrNone("GEOTRELLIS_KRYO_REGREQ") != None) {

      kryo.register(classOf[geotrellis.spark.util.OptimusPrime])
      kryo.register(classOf[scala.reflect.ClassTag$$anon$1])
      kryo.register(classOf[Array[Boolean]])
      kryo.register(classOf[java.util.ArrayList[_]])

    /* Special Handling: Avro */
      kryo.register(new Field("a", Schema.create(Type.NULL), null, null: Object).order.getClass)
      classOf[org.apache.avro.Schema]
        .getDeclaredClasses
        .foreach({ c => kryo.register(c) })
    }
  }
}
