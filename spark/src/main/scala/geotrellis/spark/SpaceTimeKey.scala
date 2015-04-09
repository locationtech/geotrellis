package geotrellis.spark

import geotrellis.spark.io.json._
import monocle._

import org.joda.time.DateTime

import spray.json._
import spray.json.DefaultJsonProtocol._

// TODO: Change this to be col, row, time, and have the compenent keys derived.
case class SpaceTimeKey(spatialKey: SpatialKey, temporalKey: TemporalKey)

object SpaceTimeKey {
  implicit object SpatialComponent extends SpatialComponent[SpaceTimeKey] {
    def lens =  createLens(k => k.spatialKey, sk => k => SpaceTimeKey(sk, k.temporalKey))
  }

  implicit object TemporalComponent extends TemporalComponent[SpaceTimeKey] {
    def lens = createLens(k => k.temporalKey, tk => k => SpaceTimeKey(k.spatialKey, tk))
  }

  implicit def ordering: Ordering[SpaceTimeKey] =
    Ordering.by(stk => (stk.spatialKey, stk.temporalKey))

  def apply(col: Int, row: Int, time: DateTime): SpaceTimeKey =
    SpaceTimeKey(SpatialKey(col, row), TemporalKey(time))

  implicit object SpaceTimeKeyFormat extends RootJsonFormat[SpaceTimeKey] {
    def write(key: SpaceTimeKey) =
      JsObject(
        "col" -> JsNumber(key.spatialKey.col),
        "row" -> JsNumber(key.spatialKey.row),
        "time" -> key.temporalKey.time.toJson
      )

    def read(value: JsValue): SpaceTimeKey =
      value.asJsObject.getFields("col", "row", "time") match {
        case Seq(JsNumber(col), JsNumber(row), time) =>
          SpaceTimeKey(col.toInt, row.toInt, time.convertTo[DateTime])
        case _ =>
          throw new DeserializationException("SpatialKey expected")
      }
  }
}
