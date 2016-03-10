package geotrellis.spark.io.json

import geotrellis.spark._

import spray.json._
import spray.json.DefaultJsonProtocol._

object KeyFormats extends KeyFormats

trait KeyFormats {
  implicit object GridKeyFormat extends RootJsonFormat[GridKey] {
    def write(key: GridKey) =
      JsObject(
        "col" -> JsNumber(key.col),
        "row" -> JsNumber(key.row)
      )

    def read(value: JsValue): GridKey =
      value.asJsObject.getFields("col", "row") match {
        case Seq(JsNumber(col), JsNumber(row)) =>
          GridKey(col.toInt, row.toInt)
        case _ =>
          throw new DeserializationException("GridKey expected")
      }
  }

  implicit object GridTimeKeyFormat extends RootJsonFormat[GridTimeKey] {
    def write(key: GridTimeKey) =
      JsObject(
        "col" -> JsNumber(key.col),
        "row" -> JsNumber(key.row),
        "instant" -> JsNumber(key.instant)
      )

    def read(value: JsValue): GridTimeKey =
      value.asJsObject.getFields("col", "row", "instant") match {
        case Seq(JsNumber(col), JsNumber(row), JsNumber(time)) =>
          GridTimeKey(col.toInt, row.toInt, time.toLong)
        case _ =>
          throw new DeserializationException("GridKey expected")
      }
  }


  implicit object TemporalKeyFormat extends RootJsonFormat[TemporalKey] {
    def write(key: TemporalKey) =
      JsObject(
        "instant" -> JsNumber(key.instant)
      )

    def read(value: JsValue): TemporalKey =
      value.asJsObject.getFields("instant") match {
        case Seq(JsNumber(time)) =>
          TemporalKey(time.toLong)
        case _ =>
          throw new DeserializationException("TemporalKey expected")
      }
  }

  implicit def keyBoundsFormat[K: JsonFormat]: RootJsonFormat[KeyBounds[K]] =
    new RootJsonFormat[KeyBounds[K]] {
      def write(keyBounds: KeyBounds[K]) =
        JsObject(
          "minKey" -> keyBounds.minKey.toJson,
          "maxKey" -> keyBounds.maxKey.toJson
        )

      def read(value: JsValue): KeyBounds[K] =
        value.asJsObject.getFields("minKey", "maxKey") match {
          case Seq(minKey, maxKey) =>
            KeyBounds(minKey.convertTo[K], maxKey.convertTo[K])
          case _ =>
            throw new DeserializationException("${classOf[KeyBounds[K]] expected")
        }
    }
}
