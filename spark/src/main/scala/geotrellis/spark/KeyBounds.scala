package geotrellis.spark

import geotrellis.raster.GridBounds
import geotrellis.util._

import org.apache.spark.rdd.RDD

sealed trait Bounds[+A] extends Product with Serializable {
  def isEmpty: Boolean

  def nonEmpty: Boolean = ! isEmpty

  def include[B >: A](key: B)(implicit b: Boundable[B]): KeyBounds[B]

  def includes[B >: A](key: B)(implicit b: Boundable[B]): Boolean

  def combine[B >: A](other: Bounds[B])(implicit b: Boundable[B]): Bounds[B]

  def contains[B >: A](other: Bounds[B])(implicit b: Boundable[B]): Boolean

  def intersect[B >: A](other: Bounds[B])(implicit b: Boundable[B]): Bounds[B]

  def intersects[B >: A](other: KeyBounds[B])(implicit b: Boundable[B]): Boolean =
    intersect(other).nonEmpty

  def get: KeyBounds[A]

  def getOrElse[B >: A](default: => KeyBounds[B]): KeyBounds[B] =
    if (isEmpty) default else this.get

  @inline
  final def map[B](f: KeyBounds[A] => KeyBounds[B]): Bounds[B] =
    if (isEmpty)
      EmptyBounds
    else {
      f(get)
    }

  @inline
  final def flatMap[B](f: KeyBounds[A] => Bounds[B]): Bounds[B] =
    if (isEmpty)
      EmptyBounds
    else {
      f(get)
    }

  def setSpatialBounds[B >: A](other: KeyBounds[SpatialKey])(implicit ev: SpatialComponent[B]): Bounds[B]
}

object Bounds {
  def apply[A](min: A, max: A): Bounds[A] = KeyBounds(min, max)

  def fromRdd[K: Boundable, V](rdd: RDD[(K, V)]): Bounds[K] =
    rdd
      .map{ case (k, tile) => Bounds(k, k) }
      .fold(EmptyBounds) { _ combine  _ }

  implicit def toIterableKeyBounds[K](b: Bounds[K]): Iterable[KeyBounds[K]] =
    b match {
      case kb: KeyBounds[K] => Seq(kb)
      case EmptyBounds => Seq()
    }
}

case object EmptyBounds extends Bounds[Nothing] {
  def isEmpty = true

  def include[B](key: B)(implicit b: Boundable[B]): KeyBounds[B] =
    KeyBounds(key, key)

  def includes[B](key: B)(implicit b: Boundable[B]): Boolean =
    false

  def combine[B](other: Bounds[B])(implicit b: Boundable[B]): Bounds[B] =
    other

  def contains[B](other: Bounds[B])(implicit b: Boundable[B]): Boolean =
    false

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

  def contains[B >: K](other: Bounds[B])(implicit b: Boundable[B]): Boolean =
    other match {
      case KeyBounds(otherMinKey, otherMaxKey) =>
        minKey == b.minBound(minKey, otherMinKey) && maxKey == b.maxBound(maxKey, otherMaxKey)
      case EmptyBounds =>
        true
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

  def setSpatialBounds[B >: K](other: KeyBounds[SpatialKey])(implicit ev: SpatialComponent[B]): KeyBounds[B] =
    KeyBounds((minKey: B).setComponent(other.minKey), (maxKey: B).setComponent(other.maxKey))

  def setSpatialBounds[B >: K](gb: GridBounds)(implicit ev: SpatialComponent[B]): KeyBounds[B] =
    setSpatialBounds[B](KeyBounds(SpatialKey(gb.colMin, gb.rowMin), SpatialKey(gb.colMax, gb.rowMax)))
}

object KeyBounds {
  def apply(gridBounds: GridBounds): KeyBounds[SpatialKey] =
    KeyBounds(SpatialKey(gridBounds.colMin, gridBounds.rowMin), SpatialKey(gridBounds.colMax, gridBounds.rowMax))

  def includeKey[K: Boundable](seq: Seq[KeyBounds[K]], key: K) = {
    seq
      .map { kb => kb.includes(key) }
      .foldLeft(false)(_ || _)
  }

  implicit class KeyBoundsSeqMethods[K: Boundable](seq: Seq[KeyBounds[K]]) {
    def includeKey(key: K): Boolean = {
      seq
        .map { kb => kb.includes(key) }
        .foldLeft(false)(_ || _)
    }
  }

  implicit def keyBoundsToTuple[K](keyBounds: KeyBounds[K]): (K, K) = (keyBounds.minKey, keyBounds.maxKey)

  implicit class withSpatialComponentKeyBoundsMethods[K: SpatialComponent](val self: KeyBounds[K]) extends MethodExtensions[KeyBounds[K]] {
    def toGridBounds(): GridBounds = {
      val SpatialKey(minCol, minRow) = self.minKey.getComponent[SpatialKey]
      val SpatialKey(maxCol, maxRow) = self.maxKey.getComponent[SpatialKey]
      GridBounds(minCol, minRow, maxCol, maxRow)
    }
  }
}
