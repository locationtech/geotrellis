package geotrellis.raster.render

import geotrellis.raster._
import geotrellis.util._

import spire.algebra._
import spire.math.Sorting
import spire.std.any._
import spire.syntax.order._

import scala.specialized

// --- //

// TODO Experiment with this being a `trait` (for performance)
/** A `Map` which provides specific Binary Search-based ''map'' behaviour
  * with breaks and a break strategy.
  *
  * {{{
  * val bm: BreakMap = ...
  * val t: Tile = ...
  *
  * // Map all the cells of `t` to a target bin value in O(klogn).
  * val newT: Tile = t.mapWith(vm)
  * }}}
  *
  * '''Note:''' `A` and `B` are specialized on `Int` and `Double`.
  */
abstract class BreakMap[
  @specialized(Int, Double) A: Order,
  @specialized(Int, Double) B: Order
](
  breakMap: Map[A, B],
  boundary: ClassBoundaryType,
  noDataValue: B,
  fallbackValue: B,
  strict: Boolean
) {

  /** A local `isNoData`, to get around macro issues. */
  def noDataCheck(a: A): Boolean

  /* A Binary Tree of the mappable values */
  private lazy val vmTree: BTree[(A, B)] = {
    val a: Array[(A, B)] = breakMap.toArray

    Sorting.quickSort(a)

    BTree.fromSortedSeq(a.toIndexedSeq).get
  }

  /* Yield a btree search predicate function based on boundary type options. */
  private val branchPred: (A, BTree[(A, B)]) => Either[Option[BTree[(A, B)]], (A, B)] = {
    boundary match {
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

  def map(z: A): B = {
    if (noDataCheck(z)) {
      noDataValue
    } else {
      vmTree.searchWith(z, branchPred) match {
        case Some((_, v)) => v
        case None if strict => sys.error(s"Value ${z} did not have an associated value.")
        case _ => fallbackValue
      }
    }
  }
}

// TODO Clean up inheritance mechanism.
class I2IBreakMap(breakMap: Map[Int, Int]) extends BreakMap[Int, Int](
  breakMap, LessThanOrEqualTo, 0x00000000, 0x00000000, false
) {
  def noDataCheck(a: Int): Boolean = isNoData(a)
}

class I2DBreakMap(breakMap: Map[Int, Double]) extends BreakMap[Int, Double](
  breakMap, LessThanOrEqualTo, Double.NaN, Double.NaN, false
) {
  def noDataCheck(a: Int): Boolean = isNoData(a)
}

class D2DBreakMap(breakMap: Map[Double, Double]) extends BreakMap[Double, Double](
  breakMap, LessThanOrEqualTo, Double.NaN, Double.NaN, false
) {
  def noDataCheck(a: Double): Boolean = isNoData(a)
}

class D2IBreakMap(breakMap: Map[Double, Int]) extends BreakMap[Double, Int](
  breakMap, LessThanOrEqualTo, 0x00000000, 0x00000000, false
) {
  def noDataCheck(a: Double): Boolean = isNoData(a)
}

/** Helper methods for constructing BreakMaps. */
object BreakMap {
  def i2i(m: Map[Int, Int]): BreakMap[Int, Int] = new I2IBreakMap(m)

  def i2d(m: Map[Int, Double]): BreakMap[Int, Double] = new I2DBreakMap(m)

  def d2d(m: Map[Double, Double]): BreakMap[Double, Double] = new D2DBreakMap(m)

  def d2i(m: Map[Double, Int]): BreakMap[Double, Int] = new D2IBreakMap(m)
}
