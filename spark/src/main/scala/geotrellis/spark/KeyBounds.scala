package geotrellis.spark

import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect._
import scala.math.Ordering.Implicits._

case class KeyBounds[K](
  minKey: K,
  maxKey: K
)

object KeyBounds {
  def from[K: Ordering: ClassTag](rdd: RasterRDD[K]): KeyBounds[K] = {
      val initialKey: K = rdd.first._1
      val aggregateInit = (initialKey, initialKey)
      def merger(t: (K, K), v: K): (K, K) = {
        if (t._1 > v) (v, t._2)
        else if (t._2 < v) (t._1, v)
        else t
      }

      def combiner(t1: (K, K), t2: (K, K)): (K, K) = {
        (t1._1 min t2._1, t1._2 max t2._2)
      }
      val (min, max) = rdd.map(_._1).aggregate(aggregateInit)(merger, combiner)
      KeyBounds(min, max)
  }

  implicit def keyBoundsToTuple[K](keyBounds: KeyBounds[K]): (K, K) = (keyBounds.minKey, keyBounds.maxKey)

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
