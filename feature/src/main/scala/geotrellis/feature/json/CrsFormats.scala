package geotrellis.feature.json

import spray.json._

/**
 *
 */
trait CrsFormats {
  implicit object crsFormat extends RootJsonFormat[CRS] {
    override def read(json: JsValue): CRS = json.asJsObject.getFields("crs") match {
      case Seq(crs: JsObject) =>
        crs.getFields("type", "properties") match {
          case Seq(JsString("name"), props: JsObject) => props.getFields("name") match {
            case Seq(JsString(name)) => NamedCRS(name)
            case _ => throw new DeserializationException("Unable to read name property of CRS.")
          }
          case Seq(JsString("link"), props: JsObject) => props.getFields("href", "type") match {
            case Seq(JsString(href), JsString(linkType)) => LinkedCRS(href, linkType)
          }
          case _ => throw new DeserializationException("Unable to read 'type' and 'properties' of CRS")
        }
      case _ => throw new DeserializationException("Unable to find 'crs' field")
    }

    override def write(obj: CRS): JsValue = obj.toJson
  }

  implicit def crsWithFormat[T: JsonFormat] = new RootJsonFormat[WithCrs[T]] {
    override def read(json: JsValue): WithCrs[T] = {
      val t = json.convertTo[T]
      val crs = json.convertTo[CRS]
      WithCrs[T](t, crs)
    }

    override def write(obj: WithCrs[T]): JsValue = {
      obj.crs.addTo(obj.t.toJson)
    }
  }
}

object CrsFormats extends CrsFormats
