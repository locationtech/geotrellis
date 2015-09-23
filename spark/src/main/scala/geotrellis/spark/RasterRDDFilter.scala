package geotrellis.spark

import geotrellis.raster.GridBounds
import geotrellis.vector.Extent
import com.github.nscala_time.time.Imports._
import scala.annotation.implicitNotFound

@implicitNotFound("Unable to filter ${K} by ${F}, Please provide RasterFilter[${K}, ${F}, ${T}]")
trait RasterRDDFilter[K, F, T] {
  /** Should reduce one of the dimensions in KeyBounds using information from param
    * @param metadata  RasterMetadata of the layer being filtered
    * @param kb        KeyBounds within the layer, possibly already reduce for max
    * @param param     Parameter to the filter, contains information to restrict kb
    */
  def apply(metadata: RasterMetaData, kb: KeyBounds[K], param: T): Option[KeyBounds[K]]

  import RasterRDDFilter._
  /** Applies all the filters contained in the expression tree to input KeyBounds.
    * Resulting list may be equal to or less than the number of [[Value]]s in ast. */
  def apply(metadata: RasterMetaData, kb: KeyBounds[K], ast: Expression[_, T])(implicit boundable: Boundable[K]): List[KeyBounds[K]] = {
    def flatten(metadata: RasterMetaData, kb: KeyBounds[K], ast: Expression[_, T]): List[KeyBounds[K]] =
      ast match {
        case Value(x) => apply(metadata, kb, x).toList
        case Or(v1, v2) => flatten(metadata, kb, v1) ++ flatten(metadata,kb, v2)
      }

    val keyBounds = flatten(metadata, kb, ast)
    keyBounds.combinations(2).foreach { case Seq(a, b) =>
      if (boundable.intersects(a, b))
        sys.error(s"Query expression produced intersecting bounds, only non-intersecting regions are supported. ($a, $b)")
    }

    keyBounds
  }
}

object RasterRDDFilter {
  
  /** [[Value]] and [[Or]] form the leaf and nodes of the expression tree used by the filter.
    * F should be a companion object type (ex: Intersects.type) and is used to restrict
    * combination of disjunctions to a single type.
    * T is the actual parameter value of the expression */
  sealed trait Expression[F, T] {
    def or(other: Expression[F, T]) = Or[F, T](this, other)
  }
  case class Value[F, T](value: T) extends Expression[F, T]
  case class Or[F, T](f1: Expression[F,T], f2: Expression[F,T]) extends Expression[F, T]
}


object RasterIntersects {
  def apply[T](value: T) = RasterRDDFilter.Value[RasterIntersects.type, T](value)

  /** Define Intersects filter for GridBounds */   
  implicit def forGridBounds[K: SpatialComponent: Boundable] =
    new RasterRDDFilter[K, RasterIntersects.type, GridBounds] {
      def apply(metadata: RasterMetaData, kb: KeyBounds[K], bounds: GridBounds) = {
        val queryBounds = KeyBounds(
          kb.minKey updateSpatialComponent SpatialKey(bounds.colMin, bounds.rowMin),
          kb.maxKey updateSpatialComponent SpatialKey(bounds.colMax, bounds.rowMax))
        implicitly[Boundable[K]].intersect(queryBounds, kb)
      }
    }
  
  /** Define Intersects filter for Extent */
  implicit def forExtent[K: SpatialComponent: Boundable] =
    new RasterRDDFilter[K, RasterIntersects.type, Extent] {
      def apply(metadata: RasterMetaData, kb: KeyBounds[K], extent: Extent) = {
        val bounds = metadata.mapTransform(extent)
        val queryBounds = KeyBounds(
          kb.minKey updateSpatialComponent SpatialKey(bounds.colMin, bounds.rowMin),
          kb.maxKey updateSpatialComponent SpatialKey(bounds.colMax, bounds.rowMax))
        implicitly[Boundable[K]].intersect(queryBounds, kb)
      }
    }
}

object RasterBetween {
  def apply[T](start: T, end: T) = RasterRDDFilter.Value[RasterBetween.type, (T, T)](start -> end)

  /** Define Between filter for a tuple of DateTimes */
  implicit def forDateTimeTuple[K: TemporalComponent : Boundable] =
    new RasterRDDFilter[K, RasterBetween.type, (DateTime, DateTime)] {
      def apply(metadata: RasterMetaData, kb: KeyBounds[K], range: (DateTime, DateTime)) = {
        val queryBounds = KeyBounds(
          kb.minKey updateTemporalComponent TemporalKey(range._1),
          kb.maxKey updateTemporalComponent TemporalKey(range._2))
        implicitly[Boundable[K]].intersect(queryBounds, kb)
      }
    }
}