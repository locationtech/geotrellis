package geotrellis.spark

import spray.json._
import spray.json.DefaultJsonProtocol._

case class KeyBounds[K](
  minKey: K,
  maxKey: K
) {
  def includes(key: K)(implicit b: Boundable[K]): Boolean =
    minKey == b.minBound(minKey, key) && maxKey == b.maxBound(maxKey, key)

  def include(key: K)(implicit b: Boundable[K]): KeyBounds[K] =
    KeyBounds(
      b.minBound(minKey, key),
      b.maxBound(maxKey, key))

  def combine(other: KeyBounds[K])(implicit b: Boundable[K]): KeyBounds[K] =
    KeyBounds(
      b.minBound(minKey, other.minKey),
      b.maxBound(maxKey, other.maxKey))

  def intersect(other: KeyBounds[K])(implicit b: Boundable[K]): Option[KeyBounds[K]] = {
    val newMin = b.maxBound(minKey, other.minKey)
    val newMax = b.minBound(maxKey, other.maxKey)

    // Intersection may not exist
    if (b.minBound(newMin, newMax) == newMin)
      Some(KeyBounds(newMin, newMax))
    else
      None
  }

  def intersects(other: KeyBounds[K])(implicit b: Boundable[K]): Boolean = {
    intersect(other).nonEmpty
  }
}

object KeyBounds {
  def includeKey[K: Boundable](seq: Seq[KeyBounds[K]], key: K) = {
    seq
      .map{ kb => kb.includes(key) }
      .foldLeft(false)(_ || _)
  }

  implicit class KeyBoundsSeqMethods[K](seq: Seq[KeyBounds[K]]) {
    def includeKey(key: K)(implicit b: Boundable[K]): Boolean = {
      seq
        .map{ kb => kb.includes(key) }
        .foldLeft(false)(_ || _)
    }
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