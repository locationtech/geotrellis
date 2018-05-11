package geotrellis.spark.io

import spray.json._


trait LayerType {
  lazy val name = this.getClass.getName.split("\\$").last.split("\\.").last
  override def toString = name
}

object LayerType {
  def fromString(str: String): LayerType =
    str match {
      case AvroLayerType.name => AvroLayerType
      case COGLayerType.name => COGLayerType
      case _ => throw new Exception(s"Could not derive LayerType from given string: $str")
    }

  implicit object LayerTypeFormat extends RootJsonFormat[LayerType] {
      def write(layerType: LayerType) = JsString(layerType.name)

      def read(value: JsValue): LayerType =
        value match {
          case JsString(layerType) =>
            LayerType.fromString(layerType)
          case v =>
            throw new DeserializationException(s"LayerType expected, got $v")
        }
    }
}

case object AvroLayerType extends LayerType
case object COGLayerType extends LayerType
