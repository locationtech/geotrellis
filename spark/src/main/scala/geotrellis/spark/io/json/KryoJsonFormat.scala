package geotrellis.spark.io.json

import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream}

import geotrellis.spark.utils._
import org.apache.commons.codec.binary._
import org.apache.commons.io.output.ByteArrayOutputStream

import spray.json._
import sun.misc.BASE64Encoder
import scala.reflect.ClassTag

/** JsonFormat that uses Kryo to serialize the object and store it in JSON */
class KryoJsonFormat[T: ClassTag] extends RootJsonFormat[T] {

  def write(value: T) =
    JsObject(
      "serialized" -> JsString(new String(KryoSerializer.serialize(value).map(_.toChar)))
    )

  def read(value: JsValue): T =
    value.asJsObject.getFields("serialized") match {
      case Seq(JsString(s)) => KryoSerializer.deserialize(s.toCharArray.map(_.toByte))
      case _ =>
        throw new DeserializationException("${classOf[T].getName} expected.")
    }

}


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
