package geotrellis.spark.io.geomesa.kryo

import com.esotericsoftware.kryo.Kryo
import org.apache.spark.serializer.{KryoRegistrator => SparkKryoRegistrator}
import de.javakaffee.kryoserializers._

class KryoRegistrator extends SparkKryoRegistrator {
  override def registerClasses(kryo: Kryo): Unit = {
    new geotrellis.spark.io.kryo.KryoRegistrator().registerClasses(kryo)

    // SimpleFeatureType requires proper UnmodifiableCollection serializer
    UnmodifiableCollectionsSerializer.registerSerializers(kryo)
  }
}
