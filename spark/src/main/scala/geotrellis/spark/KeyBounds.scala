package geotrellis.spark

import spray.json._
import spray.json.DefaultJsonProtocol._

sealed trait Bounds[+A] extends Product with Serializable {
  def isEmpty: Boolean

  def nonEmpty: Boolean = ! isEmpty

  def include[B >: A](key: B)(implicit b: Boundable[B]): KeyBounds[B]

  def includes[B >: A](key: B)(implicit b: Boundable[B]): Boolean

  def combine[B >: A](other: Bounds[B])(implicit b: Boundable[B]): Bounds[B]

  def intersect[B >: A](other: Bounds[B])(implicit b: Boundable[B]): Bounds[B]

  def intersects[B >: A](other: KeyBounds[B])(implicit b: Boundable[B]): Boolean =
    intersect(other).nonEmpty
}

case object EmptyBounds extends Bounds[Nothing] {
  def isEmpty = true

  def include[B](key: B)(implicit b: Boundable[B]): KeyBounds[B] =
    KeyBounds(key, key)

  def includes[B](key: B)(implicit b: Boundable[B]): Boolean =
    false

  def combine[B](other: Bounds[B])(implicit b: Boundable[B]): Bounds[B] =
    other

  def intersect[B](other: Bounds[B])(implicit b: Boundable[B]): Bounds[B] =
    EmptyBounds
}

case class KeyBounds[K](
  minKey: K,
  maxKey: K
) extends Bounds[K] {
  def isEmpty = false

  def include[B >: K](key: B)(implicit b: Boundable[B]): KeyBounds[B] =
    KeyBounds(b.minBound(minKey, key), b.maxBound(maxKey, key))

  def includes[B >: K](key: B)(implicit b: Boundable[B]): Boolean =
    minKey == b.minBound(minKey, key) && maxKey == b.maxBound(maxKey, key)

  def combine[B >: K](other: Bounds[B])(implicit b: Boundable[B]): Bounds[B] =
    other match {
      case KeyBounds(otherMin, otherMax) =>
        val newMin = b.minBound(minKey, otherMin)
        val newMax = b.maxBound(maxKey, otherMax)
        KeyBounds(newMin, newMax)

      case EmptyBounds =>
        this
    }

  def combine(other: Bounds[K])(implicit b: Boundable[K]): KeyBounds[K] =
    combine[K](other).asInstanceOf[KeyBounds[K]]

  def intersect[B >: K](other: Bounds[B])(implicit b: Boundable[B]): Bounds[B] =
    other match {
      case KeyBounds(otherMin, otherMax) =>
        val newMin = b.maxBound(minKey, otherMin)
        val newMax = b.minBound(maxKey, otherMax)

        if (b.minBound(newMin, newMax) == newMin)
          KeyBounds(newMin, newMax)
        else
          EmptyBounds

      case EmptyBounds =>
        EmptyBounds
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