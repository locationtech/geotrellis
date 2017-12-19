package geotrellis.spark.io.cog

import spray.json._

case class ZoomRange(minZoom: Int, maxZoom: Int) {
  def isSingleZoom: Boolean = minZoom == maxZoom

  def slug: String = s"${minZoom}_${maxZoom}"
}

object ZoomRange {
  implicit def ordering[A <: ZoomRange]: Ordering[A] = Ordering.by(_.maxZoom)

  implicit object ZoomRangeFormat extends RootJsonFormat[ZoomRange] {
      def write(zr: ZoomRange) =
        JsObject(
          "minZoom" -> JsNumber(zr.minZoom),
          "maxZoom" -> JsNumber(zr.maxZoom)
        )

      def read(value: JsValue): ZoomRange =
        value.asJsObject.getFields("minZoom", "maxZoom") match {
          case Seq(JsNumber(minZoom), JsNumber(maxZoom)) =>
            ZoomRange(minZoom.toInt, maxZoom.toInt)
          case v =>
            throw new DeserializationException(s"ZoomRange expected, got $v")
        }
    }
}
