package geotrellis.raster.render

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
  * val vm: ValueMap = ...
  * val t: Tile = ...
  *
  * // Map all the cells of `t` to a target bin value in O(klogn).
  * val newT: Tile = t.mapWith(vm)
  * }}}
  */
abstract class ValueMap[
  @specialized(Int, Double) A: Order,
  @specialized(Int, Double) B: Order
](
  valMap: Map[A, B],
  boundary: ClassBoundaryType,
  noDataValue: B,
  fallbackValue: B,
  strict: Boolean
) {

  /** A local `isNoData`, to get around macro issues. */
  def noDataCheck(a: A): Boolean

  /* A Binary Tree of the mappable values */
  private lazy val vmTree: BTree[(A, B)] = {
    val a: Array[(A, B)] = valMap.toArray

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
