package geotrellis.spark.io.json

import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream}
import org.apache.commons.codec.binary._
import org.apache.commons.io.output.ByteArrayOutputStream
import spray.json._
import scala.reflect.ClassTag

class JavaSerializationJsonFormat[T: ClassTag] extends RootJsonFormat[T] {
  def write(value: T) = {
    val barrOut = new ByteArrayOutputStream()
    val objOut = new ObjectOutputStream(barrOut)
    objOut.writeObject(value)
    objOut.close()
    barrOut.close()
    val base64 = Base64.encodeBase64(barrOut.toByteArray)
    JsObject(
      "obj" -> JsString(new String(base64.map(_.toChar)))
    )
  }

  def read(value: JsValue): T =
    value.asJsObject.getFields("obj") match {
      case Seq(JsString(s)) =>
        val bytes = Base64.decodeBase64(s)
        val bytesIn = new ByteArrayInputStream(bytes)
        val objIn = new ObjectInputStream(bytesIn)
        val obj = objIn.readObject().asInstanceOf[T]
        objIn.close()
        objIn.close()
        obj
      case _ =>
        throw new DeserializationException(s"{ obj: [base64] } expected: $value")
    }

}
