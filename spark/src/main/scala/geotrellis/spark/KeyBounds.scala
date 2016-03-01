package geotrellis.spark

import geotrellis.raster.GridBounds
import org.apache.spark.rdd.RDD
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

  def get: KeyBounds[A] 

  def getOrElse[B >: A](default: => KeyBounds[B]): KeyBounds[B] = 
    if (isEmpty) default else this.get

  @inline final def map[B >: A](f: (A, A) => (B, B)): Bounds[B] =
    if (isEmpty)
      EmptyBounds
    else {
      val kb = get
      val (newMin, newMax) = f(kb.minKey, kb.maxKey)
      KeyBounds(newMin, newMax)
    }

  @inline final def flatMap[B >: A](f: (A, A) => Bounds[B]): Bounds[B] =
    if (isEmpty)
      EmptyBounds
    else {
      val kb = get
      f(kb.minKey, kb.maxKey)
    }

  def setSpatialBounds[B >: A](other: KeyBounds[SpatialKey])(implicit ev: SpatialComponent[B]): Bounds[B]
}

object Bounds{
  def apply[A](min: A, max: A): Bounds[A] = KeyBounds(min, max)

  def fromRdd[K: Boundable, V](rdd: RDD[(K, V)]): Bounds[K] =
    rdd
      .map{ case (k, tile) => Bounds(k, k) }
      .fold(EmptyBounds) { _ combine  _ }
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

  def get = throw new NoSuchElementException("EmptyBounds.get")

  def setSpatialBounds[B](other: KeyBounds[SpatialKey])(implicit ev: SpatialComponent[B]): Bounds[B] =
    this
}

case class KeyBounds[+K](
  minKey: K,
  maxKey: K
) extends Bounds[K] {
  def isEmpty = false

  def include[B >: K](key: B)(implicit b: Boundable[B]): KeyBounds[B] =
    KeyBounds(b.minBound(minKey, key), b.maxBound(maxKey, key))

  def includes[B >: K](key: B)(implicit b: Boundable[B]): Boolean =
    minKey == b.minBound(minKey, key) && maxKey == b.maxBound(maxKey, key)

  def combine[B >: K](other: Bounds[B])(implicit b: Boundable[B]): KeyBounds[B] =
    other match {
      case KeyBounds(otherMin, otherMax) =>
        val newMin = b.minBound(minKey, otherMin)
        val newMax = b.maxBound(maxKey, otherMax)
        KeyBounds(newMin, newMax)

      case EmptyBounds =>
        this
    }

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

  def get = this

  def setSpatialBounds[B >: K](other: KeyBounds[SpatialKey])(implicit ev: SpatialComponent[B]) =
    KeyBounds(ev.lens.set(other.minKey)(minKey), ev.lens.set(other.maxKey)(maxKey))
}

object KeyBounds {
  def apply(gridBounds: GridBounds): KeyBounds[SpatialKey] =
    KeyBounds(SpatialKey(gridBounds.colMin, gridBounds.rowMin), SpatialKey(gridBounds.colMax, gridBounds.rowMax))

  def includeKey[K: Boundable](seq: Seq[KeyBounds[K]], key: K) = {
    seq
      .map { kb => kb.includes(key) }
      .foldLeft(false)(_ || _)
  }

  implicit class KeyBoundsSeqMethods[K](seq: Seq[KeyBounds[K]]) {
    def includeKey(key: K)(implicit b: Boundable[K]): Boolean = {
      seq
        .map { kb => kb.includes(key) }
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

