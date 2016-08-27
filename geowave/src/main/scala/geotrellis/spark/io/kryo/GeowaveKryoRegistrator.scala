package geotrellis.spark.io.kryo

import com.esotericsoftware.kryo.io.{ Input, Output }
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }
import java.io.{ ObjectInputStream, ObjectOutputStream }
import mil.nga.giat.geowave.core.index.{ Persistable, PersistenceUtils }
import org.apache.accumulo.core.data.Key
import org.geotools.coverage.grid.GridCoverage2D


// GeoWave registrator
class GeowaveKryoRegistrator extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) = {
    kryo.addDefaultSerializer(classOf[Persistable], new PersistableSerializer())
    kryo.addDefaultSerializer(classOf[GridCoverage2D], new GridCoverage2DSerializer())
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

  // Serializer for GridCoverage2D.  Delegate to Java Serialization.
  private class GridCoverage2DSerializer extends Serializer[GridCoverage2D] {
    override def write(kryo: Kryo, output: Output, gc: GridCoverage2D): Unit = {
      val bs = new ByteArrayOutputStream
      val oos = new ObjectOutputStream(bs)

      oos.writeObject(gc)

      val bytes = bs.toByteArray

      output.writeInt(bytes.length)
      output.writeBytes(bytes)
      bs.close ; oos.close
    }

    override def read(kryo: Kryo, input: Input, t: Class[GridCoverage2D]): GridCoverage2D = {
      val length = input.readInt
      val bytes = new Array[Byte](length)

      input.read(bytes)

      val bs = new ByteArrayInputStream(bytes)
      val ois = new ObjectInputStream(bs)
      val gc = ois.readObject.asInstanceOf[GridCoverage2D]

      bs.close ; ois.close
      gc
    }
  }

}
