package geotrellis.spark.io

import com.github.nscala_time.time.Imports._
import geotrellis.raster.GridBounds
import geotrellis.spark._
import geotrellis.vector.Extent

import scala.annotation.implicitNotFound

@implicitNotFound("Unable to filter ${K} by ${F} given ${M}, Please provide RDDFilter[${K}, ${F}, ${T}, ${M}]")
trait RDDFilter[K, F, T, M] {
  /** Should reduce one of the dimensions in KeyBounds using information from param
    * @param metadata  M of the layer being filtered
    * @param kb        KeyBounds within the layer, possibly already reduce for max
    * @param param     Parameter to the filter, contains information to restrict kb
    */
  def apply(metadata: M, kb: KeyBounds[K], param: T): Option[KeyBounds[K]]

  import RDDFilter._
  /** Applies all the filters contained in the expression tree to input KeyBounds.
    * Resulting list may be equal to or less than the number of [[Value]]s in ast. */
  def apply(metadata: M, kb: KeyBounds[K], ast: Expression[_, T]): List[KeyBounds[K]] = {
    def flatten(metadata: M, kb: KeyBounds[K], ast: Expression[_, T]): List[KeyBounds[K]] =
      ast match {
        case Value(x) => apply(metadata, kb, x).toList
        case Or(v1, v2) => flatten(metadata, kb, v1) ++ flatten(metadata,kb, v2)
      }

    val keyBounds = flatten(metadata, kb, ast)
    keyBounds.combinations(2).foreach { case Seq(a, b) =>
      if (a intersects b)
        sys.error(s"Query expression produced intersecting bounds, only non-intersecting regions are supported. ($a, $b)")
    }

    keyBounds
  }
}

object RDDFilter {

  /** [[Value]] and [[Or]] form the leaf and nodes of the expression tree used by the filter.
    * F should be a companion object type (ex: RddIntersects.type) and is used to restrict
    * combination of disjunctions to a single type.
    * T is the actual parameter value of the expression */
  sealed trait Expression[F, T] {
    def or(other: Expression[F, T]) = Or[F, T](this, other)
  }
  case class Value[F, T](value: T) extends Expression[F, T]
  case class Or[F, T](f1: Expression[F,T], f2: Expression[F,T]) extends Expression[F, T]
}


object Intersects {
  def apply[T](value: T) = RDDFilter.Value[Intersects.type, T](value)

  /** Define Intersects filter for KeyBounds */
  implicit def forKeyBounds[K: Boundable, M] =
    new RDDFilter[K, Intersects.type, KeyBounds[K], M] {
      def apply(metadata: M, kb1: KeyBounds[K], kb2: KeyBounds[K]) = {
        kb2 intersect kb1
      }
    }

  /** Define Intersects filter for GridBounds */
  implicit def forGridBounds[K: SpatialComponent: Boundable, M] =
    new RDDFilter[K, Intersects.type, GridBounds, M] {
      def apply(metadata: M, kb: KeyBounds[K], bounds: GridBounds) = {
        val queryBounds = KeyBounds(
          kb.minKey updateSpatialComponent SpatialKey(bounds.colMin, bounds.rowMin),
          kb.maxKey updateSpatialComponent SpatialKey(bounds.colMax, bounds.rowMax))
        queryBounds intersect kb
      }
    }

  /** Define Intersects filter for Extent */
  implicit def forExtent[K: SpatialComponent: Boundable, M] =
    new RDDFilter[K, Intersects.type, Extent, M] {
    def apply(metadata: M, kb: KeyBounds[K], extent: Extent) = {
      // TODO: Stopgap. We can not ask for an implicit for PDT, so we do this cast. Safe while there is only one type of Metadata.
      val bounds = metadata.asInstanceOf[RasterMetaData].mapTransform(extent)
      val queryBounds = KeyBounds(
        kb.minKey updateSpatialComponent SpatialKey(bounds.colMin, bounds.rowMin),
        kb.maxKey updateSpatialComponent SpatialKey(bounds.colMax, bounds.rowMax))
      queryBounds intersect kb
    }
  }
}

object Between {
  def apply[T](start: T, end: T) = RDDFilter.Value[Between.type, (T, T)](start -> end)

  /** Define Between filter for a tuple of DateTimes */
  implicit def forDateTimeTuple[K: TemporalComponent : Boundable, M] =
    new RDDFilter[K, Between.type, (DateTime, DateTime), M] {
      def apply(metadata: M, kb: KeyBounds[K], range: (DateTime, DateTime)) = {
        val queryBounds = KeyBounds(
          kb.minKey updateTemporalComponent TemporalKey(range._1),
          kb.maxKey updateTemporalComponent TemporalKey(range._2))
        queryBounds intersect kb
      }
    }
}