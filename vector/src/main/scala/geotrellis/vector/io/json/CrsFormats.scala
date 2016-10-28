package geotrellis.vector.io.json

import java.net.URI

import spray.json._

/** A trait specifying CRS/JSON conversion */
trait CrsFormats {

  implicit object LinkedCRSFormat extends RootJsonFormat[LinkedCRS] {
    def write(obj: LinkedCRS) = JsObject(
      "type" -> JsString("link"),
      "properties" -> JsObject(
        "href" -> JsString(obj.href.toString),
        "type" -> JsString(obj.format)
      )
    )

    def read(js: JsValue) =
      js.asJsObject.getFields("type", "properties") match {
        case Seq(JsString("link"), props: JsObject) =>
          props.getFields("href", "type") match {
            case Seq(JsString(href), JsString(linkType)) =>
              LinkedCRS(new URI(href), linkType)
            case _ =>
              throw new DeserializationException("Unable to read 'crs.properties'")
          }
        case Seq(JsString(crsType), _) =>
          throw new DeserializationException(s"Unable to read CRS of type $crsType")
      }
  }

  implicit object NamedCRSFormat extends RootJsonFormat[NamedCRS] {
    def write(obj: NamedCRS) = JsObject(
      "type" -> JsString("name"),
      "properties" -> JsObject(
        "name" -> JsString(obj.name)
      )
    )

    def read(js: JsValue) =
      js.asJsObject.getFields("type", "properties") match {
        case Seq(JsString("name"), props: JsObject) =>
          props.getFields("name") match {
            case Seq(JsString(name)) =>
              NamedCRS(name)
            case _ =>
              throw new DeserializationException("Unable to read 'crs.properties'")
          }
        case Seq(JsString(crsType), _) =>
          throw new DeserializationException(s"Unable to read CRS of type $crsType")
      }
  }

  implicit object crsFormat extends RootJsonFormat[JsonCRS] {
    override def read(json: JsValue): JsonCRS = {
      json.asJsObject.getFields("type", "properties") match {
        case Seq(JsString("name"), props: JsObject) =>
          json.convertTo[NamedCRS]
        case Seq(JsString("link"), props: JsObject) =>
          json.convertTo[LinkedCRS]
        case Seq(JsString(crsType), _) =>
          throw new DeserializationException(s"Unable to read CRS of type $crsType")
      }
    }

    override def write(obj: JsonCRS): JsValue =
      obj match {
        case crs: NamedCRS => crs.toJson
        case crs: LinkedCRS => crs.toJson
      }
  }

  implicit def withCrsFormat[T: RootJsonFormat] = new RootJsonFormat[WithCrs[T]] {
    override def read(json: JsValue): WithCrs[T] =
      WithCrs[T](
        json.convertTo[T],
        json.asJsObject.fields("crs").convertTo[JsonCRS])

    override def write(withCrs: WithCrs[T]): JsValue =
      JsObject( withCrs.obj.toJson.asJsObject.fields + ("crs" -> withCrs.crs.toJson))
  }
}

object CrsFormats extends CrsFormats
