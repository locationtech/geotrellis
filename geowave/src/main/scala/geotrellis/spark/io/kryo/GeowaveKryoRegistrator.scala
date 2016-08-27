package geotrellis.spark.io.kryo

import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import mil.nga.giat.geowave.core.index.Persistable
import mil.nga.giat.geowave.core.index.PersistenceUtils
import org.apache.accumulo.core.data.Key


// GeoWave registrator
class GeowaveKryoRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) = {
    kryo.addDefaultSerializer(classOf[Persistable], new PersistableSerializer())
    kryo.register(classOf[Key])
    super.registerClasses(kryo)
  }

  //Default serializer for any GeoWave Persistable object
  private class PersistableSerializer extends Serializer[Persistable] {
    override def write(kryo: Kryo, output: Output, geowaveObj: Persistable): Unit = {
      val bytes = PersistenceUtils.toBinary(geowaveObj)
      output.writeInt(bytes.length)
      output.writeBytes(bytes)
    }

    override def read(kryo: Kryo, input: Input, t: Class[Persistable]): Persistable = {
      val length = input.readInt()
      val bytes = new Array[Byte](length)
      input.read(bytes)

      PersistenceUtils.fromBinary(bytes, classOf[Persistable])
    }
  }
}
