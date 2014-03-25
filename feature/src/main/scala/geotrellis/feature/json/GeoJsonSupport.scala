package geotrellis.feature.json

import geotrellis.feature._

import spray.json._
import spray.http._
import spray.httpx._
import spray.httpx.marshalling.Marshaller
import spray.httpx.unmarshalling.Unmarshaller


/**
 * A trait providing automatic to and from JSON marshalling/unmarshalling using an in-scope *spray-json* protocol.
 * Note that *spray-httpx* does not have an automatic dependency on *spray-json*.
 * You'll need to provide the appropriate *spray-json* artifacts yourself.
 */
trait GeoJsonSupport extends FeatureFormats {
  implicit def sprayJsonUnmarshallerConverter[T](reader: RootJsonReader[T]) =
    sprayJsonUnmarshaller(reader)
  implicit def sprayJsonUnmarshaller[T: RootJsonReader] =
    Unmarshaller[T](MediaTypes.`application/json`) {
      case x: HttpEntity.NonEmpty ⇒
        val json = JsonParser(x.asString(defaultCharset = HttpCharsets.`UTF-8`))
        jsonReader[T].read(json)
    }
  implicit def sprayJsonMarshallerConverter[T](writer: RootJsonWriter[T])(implicit printer: JsonPrinter = PrettyPrinter) =
    sprayJsonMarshaller[T](writer, printer)
  implicit def sprayJsonMarshaller[T](implicit writer: RootJsonWriter[T], printer: JsonPrinter = PrettyPrinter, csr: CSR = NoCSR()) =
    Marshaller.delegate[T, String](ContentTypes.`application/json`) { value =>
      val json = writer.write(value)
//      val csrJson = json match {
//        case JsObject(fields) =>
//          JsObject(fields + ("CSR" -> JsString("MY CSR WILL GO WITH ME")))
//      }
      printer(json)
    }
}

object GeoJsonSupport extends GeoJsonSupport {
  implicit val defaultCSR = NoCSR
}

