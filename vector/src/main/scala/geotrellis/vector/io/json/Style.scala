package geotrellis.vector.io.json

import spray.json._
import scala.collection.mutable

case class Style(
  strokeColor: Option[String],
  strokeWidth: Option[String],
  strokeOpacity: Option[Double],
  fillColor: Option[String],
  fillOpacity: Option[Double] 
)

object Style {
  def apply(
    strokeColor: String = "",
    strokeWidth: String = "",
    strokeOpacity: Double = Double.NaN,
    fillColor: String = "",
    fillOpacity: Double = Double.NaN
  ): Style = 
    Style(
      if(strokeColor != "") Some(strokeColor) else None,
      if(strokeWidth != "") Some(strokeWidth) else None,
      if(!java.lang.Double.isNaN(strokeOpacity)) Some(strokeOpacity) else None,
      if(fillColor != "") Some(fillColor) else None,
      if(!java.lang.Double.isNaN(fillOpacity)) Some(fillOpacity) else None
    )

  implicit object StyleFormat extends RootJsonFormat[Style] {
    def read(value: JsValue): Style =
      value match {
        case obj: JsObject =>
          val fields = obj.fields
          val strokeColor =
            fields.get("stroke") match {
              case Some(JsString(v)) => Some(v)
              case None => None
              case Some(v) => throw new DeserializationException(s"'stroke' property must be a string, got $v")
            }

          val strokeWidth =
            fields.get("stroke-width") match {
              case Some(JsString(v)) => Some(v)
              case None => None
              case Some(v) => throw new DeserializationException(s"'stroke-width' property must be a string, got $v")
            }

          val strokeOpacity =
            fields.get("stroke-opacity") match {
              case Some(JsString(v)) => Some(v.toDouble)
              case None => None
              case Some(v) => throw new DeserializationException(s"'stroke-opacity' property must be a string, got $v")
            }

          val fillColor =
            fields.get("fill") match {
              case Some(JsString(sc)) => Some(sc)
              case None => None
              case Some(v) => throw new DeserializationException(s"'fill' property must be a string, got $v")
            }

          val fillOpacity =
            fields.get("fill-opacity") match {
              case Some(JsNumber(v)) => Some(v.toDouble)
              case None => None
              case Some(v) => throw new DeserializationException(s"'fill-opacity' property must be a string, got $v")
            }

          Style(strokeColor, strokeWidth, strokeOpacity, fillColor, fillOpacity)
        case _ =>
          Style()
      }

    def write(style: Style): JsValue = {
      val l = mutable.ListBuffer[(String, JsValue)]()
      if(style.strokeColor.isDefined) l += ( ("stroke", JsString(style.strokeColor.get)) )
      if(style.strokeWidth.isDefined) l += ( ("stroke-width", JsString(style.strokeWidth.get)) )
      if(style.strokeOpacity.isDefined) l += ( ("stroke-opacity", JsNumber(style.strokeOpacity.get)) )
      if(style.fillColor.isDefined) l += ( ("fill", JsString(style.fillColor.get)) )
      if(style.fillOpacity.isDefined) l += ( ("fill-opacity", JsNumber(style.fillOpacity.get)) )

      JsObject(l:_*)
    }
  }
}
