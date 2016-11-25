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

package geotrellis.spark

package object testkit {
  implicit class TestJavaSerialization[T](val t: T) {
    def serializeAndDeserialize(): T = {
      import java.io.ByteArrayInputStream
      import java.io.ByteArrayOutputStream
      import java.io.DataInputStream
      import java.io.DataOutputStream
      import java.io.ObjectInputStream
      import java.io.ObjectOutputStream

      val baos = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(baos)
      oos.writeObject(t)
      oos.close()

      val b = baos.toByteArray()
      val bais = new ByteArrayInputStream(b)
      val ois = new ObjectInputStream(bais)

      val actual = ois.readObject().asInstanceOf[T]
      ois.close()
      actual
    }
  }
}
