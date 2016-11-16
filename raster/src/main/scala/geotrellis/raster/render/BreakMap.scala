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

package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.util._

import spire.algebra._
import spire.math.Sorting
import spire.std.any._
import spire.syntax.order._

import scala.specialized

// --- //

/** Root element in hierarchy for specifying the type of boundary when classifying colors*/
sealed trait ClassBoundaryType
case object GreaterThan extends ClassBoundaryType
case object GreaterThanOrEqualTo extends ClassBoundaryType
case object LessThan extends ClassBoundaryType
case object LessThanOrEqualTo extends ClassBoundaryType
case object Exact extends ClassBoundaryType

/** A strategy for mapping values via a [[BreakMap]].
  *
  * '''Note:''' Specialized for `Int` and `Double`.
  */
class MapStrategy[@specialized(Int, Double) A](
  val boundary: ClassBoundaryType,
  val noDataValue: A,
  val fallbackValue: A,
  val strict: Boolean
) extends Serializable

/** Helper methods for constructing a [[MapStrategy]]. */
object MapStrategy {
  def int: MapStrategy[Int] = new MapStrategy(LessThanOrEqualTo, NODATA, NODATA, false)

  def double: MapStrategy[Double] = new MapStrategy(LessThanOrEqualTo, doubleNODATA, doubleNODATA, false)
}

/** A `Map` which provides specific Binary Search-based ''map'' behaviour
  * with breaks and a break strategy.
  *
  * {{{
  * val bm: BreakMap[Int, Int] = ...
  * val t: Tile = ...
  *
  * // Map all the cells of `t` to a target bin value in O(klogn).
  * val newT: Tile = t.map(vm)
  * }}}
  *
  * '''Note:''' `A` and `B` are specialized on `Int` and `Double`.
  */
class BreakMap[
  @specialized(Int, Double) A: Order,
  @specialized(Int, Double) B: Order
](
  breakMap: Map[A, B],
  strategy: MapStrategy[B],
  noDataCheck: A => Boolean
) extends (A => B) with Serializable {

  /* A Binary Tree of the mappable values */
  private lazy val vmTree: BTree[(A, B)] = {
    val a: Array[(A, B)] = breakMap.toArray

    Sorting.quickSort(a)

    BTree.fromSortedSeq(a.toIndexedSeq).get
  }

  /* Yield a btree search predicate function based on boundary type options. */
  private val branchPred: (A, BTree[(A, B)]) => Either[Option[BTree[(A, B)]], (A, B)] = {
    strategy.boundary match {
      case LessThan => { (z, tree) => tree match {
        case BTree(v, None, _)    if z < v._1                       => Right(v)
        case BTree(v, Some(l), _) if z < v._1 && z >= l.greatest._1 => Right(v)
        case BTree(v, l, _)       if z < v._1                       => Left(l)
        case BTree(_, _, r)                                         => Left(r)
      }}
      case LessThanOrEqualTo => { (z, tree) => tree match {
        case BTree(v, None, _)    if z <= v._1                      => Right(v)
        case BTree(v, Some(l), _) if z <= v._1 && z > l.greatest._1 => Right(v)
        case BTree(v, l, _)       if z < v._1                       => Left(l)
        case BTree(_, _, r)                                         => Left(r)
      }}
      case Exact => { (z, tree) => tree match { /* Vanilla Binary Search */
        case BTree(v, _, _) if z == v._1 => Right(v)
        case BTree(v, l, _) if z < v._1  => Left(l)
        case BTree(_, _, r)              => Left(r)
      }}
      case GreaterThanOrEqualTo => { (z, tree) => tree match {
        case BTree(v, _, None)    if z >= v._1                    => Right(v)
        case BTree(v, _, Some(r)) if z >= v._1 && z < r.lowest._1 => Right(v)
        case BTree(v, l, _)       if z < v._1                     => Left(l)
        case BTree(_, _, r)                                       => Left(r)
      }}
      case GreaterThan => { (z, tree) => tree match {
        case BTree(v, _, None)    if z > v._1                     => Right(v)
        case BTree(v, _, Some(r)) if z > v._1 && z <= r.lowest._1 => Right(v)
        case BTree(v, l, _)       if z <= v._1                    => Left(l) /* (<=) is correct here! */
        case BTree(_, _, r)                                       => Left(r)
      }}
    }
  }

  def apply(z: A): B = {
    if (noDataCheck(z)) {
      strategy.noDataValue
    } else {
      vmTree.searchWith(z, branchPred) match {
        case Some((_, v)) => v
        case None if strategy.strict => sys.error(s"Value ${z} did not have an associated value.")
        case _ => strategy.fallbackValue
      }
    }
  }
}

/** Helper methods for constructing BreakMaps. */
object BreakMap {
  def apply(m: Map[Int, Int]): BreakMap[Int, Int] =
    new BreakMap(m, MapStrategy.int, { i => isNoData(i) })

  def apply(m: Map[Int, Double])(implicit a: DI): BreakMap[Int, Double] =
    new BreakMap(m, MapStrategy.double, { i => isNoData(i) })

  def apply(m: Map[Double, Double])(implicit a: DI, b: DI): BreakMap[Double, Double] =
    new BreakMap(m, MapStrategy.double, { d => isNoData(d) })

  def apply(m: Map[Double, Int])(implicit a: DI, b: DI, c: DI): BreakMap[Double, Int] =
    new BreakMap(m, MapStrategy.int, { d => isNoData(d) })
}
