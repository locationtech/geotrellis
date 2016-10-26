package geotrellis.spark.io.kryo

import geotrellis.util.annotations.experimental

import com.esotericsoftware.kryo.io.{ Input, Output }
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import de.javakaffee.kryoserializers._
import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }
import java.io.{ ObjectInputStream, ObjectOutputStream }
import mil.nga.giat.geowave.core.index.{ Persistable, PersistenceUtils }
import org.apache.accumulo.core.data.Key
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.data.DataUtilities
import org.opengis.feature.simple.SimpleFeatureType


/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental class GeowaveKryoRegistrator extends KryoRegistrator {

  /** $experimental */
  @experimental override def registerClasses(kryo: Kryo) = {
    UnmodifiableCollectionsSerializer.registerSerializers(kryo)
    kryo.addDefaultSerializer(classOf[Persistable], new PersistableSerializer())
    kryo.addDefaultSerializer(classOf[GridCoverage2D], new DelegateSerializer[GridCoverage2D]())
    kryo.register(classOf[Key])
    super.registerClasses(kryo)
  }

  /** $experimental Default serializer for any GeoWave Persistable object */
  @experimental private class PersistableSerializer extends Serializer[Persistable] {
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

  /**
    *  $experimental Serializer for difficult types.  This simply
    *  delegates to Java Serialization.
    */
  @experimental private class DelegateSerializer[T] extends Serializer[T] {
    override def write(kryo: Kryo, output: Output, x: T): Unit = {
      val bs = new ByteArrayOutputStream
      val oos = new ObjectOutputStream(bs)

      oos.writeObject(x)

      val bytes = bs.toByteArray

      output.writeInt(bytes.length)
      output.writeBytes(bytes)
      bs.close ; oos.close
    }

    override def read(kryo: Kryo, input: Input, t: Class[T]): T = {
      val length = input.readInt
      val bytes = new Array[Byte](length)

      input.read(bytes)

      val bs = new ByteArrayInputStream(bytes)
      val ois = new ObjectInputStream(bs)
      val x = ois.readObject.asInstanceOf[T]

      bs.close ; ois.close
      x
    }
  }

}
