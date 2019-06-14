/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.store.kryo

import geotrellis.util.annotations.experimental

import org.apache.accumulo.core.data.Key
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.data.DataUtilities
import org.opengis.feature.simple.SimpleFeatureType

import com.esotericsoftware.kryo.io.{ Input, Output }
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer

import de.javakaffee.kryoserializers._

import mil.nga.giat.geowave.core.index.{ Persistable, PersistenceUtils }

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }
import java.io.{ ObjectInputStream, ObjectOutputStream }


/**
  * @define experimental <span class="badge badge-red" style="float: right;">EXPERIMENTAL</span>@experimental
  */
@experimental class GeoWaveKryoRegistrator extends KryoRegistrator {

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
