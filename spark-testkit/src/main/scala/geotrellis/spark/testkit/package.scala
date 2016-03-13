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
