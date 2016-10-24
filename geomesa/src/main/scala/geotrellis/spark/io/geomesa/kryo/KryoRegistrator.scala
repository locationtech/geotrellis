package geotrellis.spark.io.geomesa.kryo

import geotrellis.util.annotations.experimental

import com.esotericsoftware.kryo.Kryo
import org.apache.spark.serializer.{KryoRegistrator => SparkKryoRegistrator}
import de.javakaffee.kryoserializers._


/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental class KryoRegistrator extends SparkKryoRegistrator {

  /** $experimental */
  @experimental override def registerClasses(kryo: Kryo): Unit = {
    new geotrellis.spark.io.kryo.KryoRegistrator().registerClasses(kryo)

    // SimpleFeatureType requires proper UnmodifiableCollection serializer
    UnmodifiableCollectionsSerializer.registerSerializers(kryo)
  }
}
