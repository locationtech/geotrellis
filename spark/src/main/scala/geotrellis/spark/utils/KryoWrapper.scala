/*
 * Copyright (C) 2012 The Regents of The University California.
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.utils

import scala.reflect.ClassTag

/**
 * A wrapper around some unserializable objects that make them both Java
 * serializable. Internally, Kryo is used for serialization.
 *
 * Use KryoWrapper(value) to create a wrapper.
 */
class KryoWrapper[T: ClassTag] extends Serializable {

  @transient var value: T = _

  private var valueSerialized: Array[Byte] = _

  // The getter and setter for valueSerialized is used for XML serialization.
  def getValueSerialized(): Array[Byte] = {
    valueSerialized = KryoSerializer.serialize(value)
    valueSerialized
  }

  def setValueSerialized(bytes: Array[Byte]) = {
    valueSerialized = bytes
    value = KryoSerializer.deserialize[T](valueSerialized)
  }

  // Used for Java serialization.
  private def writeObject(out: java.io.ObjectOutputStream) {
    getValueSerialized()
    out.defaultWriteObject()
  }

  private def readObject(in: java.io.ObjectInputStream) {
    in.defaultReadObject()
    setValueSerialized(valueSerialized)
  }

}

object KryoWrapper {
  def apply[T: ClassTag](value: T): KryoWrapper[T] = {
    val wrapper = new KryoWrapper[T]
    wrapper.value = value
    wrapper
  }
}
