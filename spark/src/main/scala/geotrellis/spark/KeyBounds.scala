/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark

import geotrellis.raster.{GridBounds, RasterExtent}
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._

import cats.Functor
import org.apache.spark.rdd.RDD

/** Represents a region of discrete space, bounding it by minimum and maximum points.
 * The bounds maybe [[EmptyBounds]] as result of intersection operation.
 *
 * The dimensionality of region is implied by the dimensionality of type parameter A.
 * [[Boundable]] typeclass is required to manipulate instance of A.
 *
 * Conceptually this ADT is similar `Option[KeyBounds[A]]` but adds methods convenient
 * for testing and forming region intersection, union and mutation.
 *
 * @tparam A Type of keys, or points in descrete space
 */
sealed trait Bounds[+A] extends Product with Serializable {
  /** Returns true if this is [[EmptyBounds]] */
  def isEmpty: Boolean

  /** Returns false if this is [[EmptyBounds]] */
  def nonEmpty: Boolean = ! isEmpty

  /** Expand bounds to include the key or keep unchanged if it is already included */
  def include[B >: A](key: B)(implicit b: Boundable[B]): KeyBounds[B]

  /** Test if the key is included in bounds */
  def includes[B >: A](key: B)(implicit b: Boundable[B]): Boolean

  /** Combine two regions by creating a region that covers both regions fully */
  def combine[B >: A](other: Bounds[B])(implicit b: Boundable[B]): Bounds[B]

  /** Test if other bounds are fully contained by this bounds.
   * [[EmptyBounds]] contain no other bounds but are contained by all non-empty bounds.
   */
  def contains[B >: A](other: Bounds[B])(implicit b: Boundable[B]): Boolean

  /** Returns the intersection, if any, between two bounds */
  def intersect[B >: A](other: Bounds[B])(implicit b: Boundable[B]): Bounds[B]

  /** Test if two bounds for intersection */
  def intersects[B >: A](other: KeyBounds[B])(implicit b: Boundable[B]): Boolean =
    intersect(other).nonEmpty

  /** Returns non-empty bounds or throws [[NoSuchElementException]] */
  def get: KeyBounds[A]

  /** Returns non-empty bounds or the default value */
  def getOrElse[B >: A](default: => KeyBounds[B]): KeyBounds[B] =
    if (isEmpty) default else this.get

  /** Returns the result of applying f to this [[Bounds]] minKey and maxKey if this it is nonempty.
   * The minKey and maxKeys are given as instance of [[KeyBounds]] instead of a tuple.
   * If this [[Bounds]] is [[EmptyBounds]] it is returned unchanged.
   */
 @inline
 final def flatMap[B](f: KeyBounds[A] => Bounds[B]): Bounds[B] =
   if (isEmpty)
     EmptyBounds
   else {
     f(get)
   }

  /** Updates the spatial region of bounds to match that of the argument,
   *  leaving other dimensions, if any, unchanged.
   */
  def setSpatialBounds[B >: A](other: KeyBounds[SpatialKey])(implicit ev: SpatialComponent[B]): Bounds[B]

  def toOption: Option[KeyBounds[A]]
}

object Bounds {
  def apply[A](min: A, max: A): Bounds[A] = KeyBounds(min, max)

  def fromRDD[K: Boundable, V](rdd: RDD[(K, V)]): Bounds[K] =
    rdd
      .map{ case (k, tile) => Bounds(k, k) }
      .fold(EmptyBounds) { _ combine  _ }

  implicit val boundsFunctor: Functor[Bounds] = new Functor[Bounds] {
    override def map[A, B](fa: Bounds[A])(f: A => B): Bounds[B] = fa match {
      case EmptyBounds => EmptyBounds
      case KeyBounds(min, max) => KeyBounds(f(min), f(max))
    }
  }

  implicit def toIterableKeyBounds[K](b: Bounds[K]): Iterable[KeyBounds[K]] =
    b match {
      case kb: KeyBounds[K] => Seq(kb)
      case EmptyBounds => Seq()
    }
}

/** Represents empty region of space.
 * Empty region contains no possible key.
 */
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

  def toOption = None
}

/** Represents non-empty region of descrete space.
 * Any key which is greater than or equal to minKey and less then or equal to maxKey
 * in each individual dimension is part of the region described by these [[Bounds]].
 *
 * @param minKey Minimum key of the region, inclusive.
 * @param maxKey Maximum key of the region, inclusive.
 */
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

  def toOption: Option[KeyBounds[K]] = Some(this)

  def rekey[B >: K: SpatialComponent](sourceLayout: LayoutDefinition, targetLayout: LayoutDefinition): KeyBounds[B] = {
    val extent = sourceLayout.extent
    val sourceRe = RasterExtent(extent, sourceLayout.layoutCols, sourceLayout.layoutRows)
    val targetRe = RasterExtent(extent, targetLayout.layoutCols, targetLayout.layoutRows)

    val minSpatialKey = (minKey: B).getComponent[SpatialKey]
    val (minCol, minRow) = {
      val (x, y) = sourceRe.gridToMap(minSpatialKey.col, minSpatialKey.row)
      targetRe.mapToGrid(x, y)
    }

    val maxSpatialKey = (maxKey: B).getComponent[SpatialKey]
    val (maxCol, maxRow) = {
      val (x, y) = sourceRe.gridToMap(maxSpatialKey.col, maxSpatialKey.row)
      targetRe.mapToGrid(x, y)
    }

    KeyBounds(
      (minKey: B).setComponent(SpatialKey(minCol, minRow)),
      (maxKey: B).setComponent(SpatialKey(maxCol, maxRow))
    )
  }
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

    def toSpatial: KeyBounds[SpatialKey] = {
      val GridBounds(minCol, minRow, maxCol, maxRow) = toGridBounds()
      KeyBounds(SpatialKey(minCol, minRow), SpatialKey(maxCol, maxRow))
    }
  }
}
