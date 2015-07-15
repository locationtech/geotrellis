package geotrellis.spark.io.json

import geotrellis.spark.utils._

import spray.json._
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

