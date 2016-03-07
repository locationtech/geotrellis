package geotrellis.spark

import geotrellis.spark.io.json.Implicits._

import com.github.nscala_time.time.Imports._
import spray.json._

object TemporalKey {
  def apply(dateTime: DateTime): TemporalKey =
    TemporalKey(dateTime.getMillis)

  implicit object TemporalComponent extends IdentityComponent[TemporalKey]

  implicit def dateTimeToKey(time: DateTime): TemporalKey =
    TemporalKey(time)

  implicit def keyToDateTime(key: TemporalKey): DateTime =
    key.time

  implicit def ordering[A <: TemporalKey]: Ordering[A] =
    Ordering.by(tk => tk.instant)

  implicit object TemporalKeyFormat extends RootJsonFormat[TemporalKey] {
    def write(key: TemporalKey) =
      JsObject(
        "time" -> key.time.toJson
      )

    def read(value: JsValue): TemporalKey =
      value.asJsObject.getFields("time") match {
        case Seq(time) =>
          TemporalKey(time.convertTo[DateTime])
        case _ =>
          throw new DeserializationException("TemporalKey expected")
      }
  }
}

/** A TemporalKey designates the temporal positioning of a layer's tile. */
case class TemporalKey(instant: Long) {
  def time: DateTime = new DateTime(instant, DateTimeZone.UTC)
}
