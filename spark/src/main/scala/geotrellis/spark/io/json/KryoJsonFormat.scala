package geotrellis.spark.io.json

import geotrellis.spark.utils._

import spray.json._
import scala.reflect._
import sun.misc.{BASE64Encoder, BASE64Decoder}

/** JsonFormat that uses Kryo to serialize the object and store it in JSON */
class KryoJsonFormat[T: ClassTag] extends RootJsonFormat[T] {

 def write(value: T) =
   JsObject(
     "serialized" -> JsString(
       new BASE64Encoder().encode(
         KryoSerializer.serialize(value)
       )
     )
   )

  def read(value: JsValue): T =
    value.asJsObject.getFields("serialized") match {
      case Seq(JsString(s)) =>
        KryoSerializer.deserialize(
          new BASE64Decoder().decodeBuffer(s)
        )
      case _ =>
        throw new DeserializationException("${classOf[T].getName} expected.")
    }

}

