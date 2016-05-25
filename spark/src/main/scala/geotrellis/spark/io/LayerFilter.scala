package geotrellis.spark.io

import geotrellis.raster._
import geotrellis.raster.{GridBounds, RasterExtent, PixelIsArea}
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.vector.{Extent, Point, MultiPolygon}
import geotrellis.util._

import com.github.nscala_time.time.Imports._

import scala.annotation.implicitNotFound
import scala.collection.mutable


@implicitNotFound("Unable to filter ${K} by ${F} given ${M}, Please provide LayerFilter[${K}, ${F}, ${T}, ${M}]")
trait LayerFilter[K, F, T, M] {
  /** Should reduce one of the dimensions in KeyBounds using information from param
    *
    * @param metadata  M of the layer being filtered
    * @param kb        KeyBounds within the layer, possibly already reduce for max
    * @param param     Parameter to the filter, contains information to restrict kb
    */
  def apply(metadata: M, kb: KeyBounds[K], param: T): Seq[KeyBounds[K]]

  import LayerFilter._
  /**
    * Applies all the filters contained in the expression tree to
    * input KeyBounds.  Resulting list may be equal to or less than
    * the number of Value objects in the AST.
    */
  def apply(metadata: M, kb: KeyBounds[K], ast: Expression[_, T])(implicit boundable: Boundable[K]): List[KeyBounds[K]] = {
    def flatten(metadata: M, kb: KeyBounds[K], ast: Expression[_, T]): Seq[KeyBounds[K]] =
      ast match {
        case Value(x) => apply(metadata, kb, x)
        case Or(v1, v2) => flatten(metadata, kb, v1) ++ flatten(metadata,kb, v2)
      }

    val keyBounds = flatten(metadata, kb, ast)
    keyBounds.combinations(2).foreach { case Seq(a, b) =>
      if (a intersects b)
        sys.error(s"Query expression produced intersecting bounds, only non-intersecting regions are supported. ($a, $b)")
    }

    keyBounds.toList
  }
}

object LayerFilter {

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


object Intersects {
  import geotrellis.raster.rasterize.{Rasterizer, Callback}
  import collection.JavaConverters._
  import java.util.concurrent.ConcurrentHashMap

  def apply[T](value: T) = LayerFilter.Value[Intersects.type, T](value)

  /** Define Intersects filter for KeyBounds */
  implicit def forKeyBounds[K: Boundable, M] =
    new LayerFilter[K, Intersects.type, KeyBounds[K], M] {
      def apply(metadata: M, kb1: KeyBounds[K], kb2: KeyBounds[K]) = {
        (kb2 intersect kb1) match {
          case kb: KeyBounds[K] => List(kb)
          case EmptyBounds => Nil
        }
      }
    }

  /** Define Intersects filter for Bounds */
  implicit def forBounds[K: Boundable, M] =
    new LayerFilter[K, Intersects.type, Bounds[K], M] {
      def apply(metadata: M, kb: KeyBounds[K], bounds: Bounds[K]) = {
        (bounds intersect kb) match {
          case kb: KeyBounds[K] => List(kb)
          case EmptyBounds => Nil
        }
      }
    }

  /** Define Intersects filter for GridBounds */
  implicit def forGridBounds[K: SpatialComponent: Boundable, M] =
    new LayerFilter[K, Intersects.type, GridBounds, M] {
      def apply(metadata: M, kb: KeyBounds[K], bounds: GridBounds) = {
        val queryBounds = KeyBounds(
          kb.minKey setComponent SpatialKey(bounds.colMin, bounds.rowMin),
          kb.maxKey setComponent SpatialKey(bounds.colMax, bounds.rowMax))
        (queryBounds intersect kb) match {
          case kb: KeyBounds[K] => List(kb)
          case EmptyBounds => Nil
        }
      }
    }

  /** Define Intersects filter for Extent */
  implicit def forExtent[K: SpatialComponent: Boundable, M: GetComponent[?, LayoutDefinition]] =
    new LayerFilter[K, Intersects.type, Extent, M] {
    def apply(metadata: M, kb: KeyBounds[K], extent: Extent) = {
      val bounds = metadata.getComponent[LayoutDefinition].mapTransform(extent)
      val queryBounds = KeyBounds(
        kb.minKey setComponent SpatialKey(bounds.colMin, bounds.rowMin),
        kb.maxKey setComponent SpatialKey(bounds.colMax, bounds.rowMax))
      (queryBounds intersect kb) match {
        case kb: KeyBounds[K] => List(kb)
        case EmptyBounds => Nil
      }
    }
  }

  /** Define Intersects filter for Polygon */
  implicit def forPolygon[K: SpatialComponent: Boundable, M: GetComponent[?, LayoutDefinition]] =
    new LayerFilter[K, Intersects.type, MultiPolygon, M] {
      def apply(metadata: M, kb: KeyBounds[K], polygon: MultiPolygon) = {
        val mapTransform = metadata.getComponent[LayoutDefinition].mapTransform
        val extent = polygon.envelope
        val keyext = mapTransform(kb.minKey)
        val bounds = mapTransform(extent)
        val options = Options(includePartial=true, sampleType=PixelIsArea)

        /*
         * Construct  a  rasterExtent  that fits  tightly  around  the
         * candidate tiles  (the candidate keys).  IT  IS ASSUMED THAT
         * ALL TILES HAVE THE SAME LENGTH AND HEIGHT.
         */
        val xmin = math.floor(extent.min.x / keyext.width) * keyext.width
        val ymin = math.floor(extent.min.y / keyext.height) * keyext.height
        val xmax = math.ceil(extent.max.x / keyext.width) * keyext.width
        val ymax = math.ceil(extent.max.y / keyext.height) * keyext.height
        val rasterExtent = RasterExtent(Extent(xmin, ymin, xmax, ymax), bounds.width, bounds.height)

        /*
         * Use the Rasterizer to construct  a list of tiles which meet
         * the  query polygon.   That list  of tiles  is stored  as an
         * array of  tuples which  is then  mapped-over to  produce an
         * array of KeyBounds.
         */
        val tiles = new ConcurrentHashMap[(Int,Int), Unit]
        val fn = new Callback {
          def apply(col : Int, row : Int): Unit = {
            val tile : (Int, Int) = (bounds.colMin + col, bounds.rowMin + row)
            tiles.put(tile, Unit)
          }
        }

        polygon.foreach(rasterExtent, options)(fn)
        tiles.keys.asScala
          .map({ tile =>
            val qb = KeyBounds(
              kb.minKey setComponent SpatialKey(tile._1, tile._2),
              kb.maxKey setComponent SpatialKey(tile._1, tile._2))
            qb intersect kb match {
              case kb: KeyBounds[K] => List(kb)
              case EmptyBounds => Nil
            }
          })
          .reduce({ (x,y) => x ++ y })
      }
    }
}

object At {
  def apply[T](at: T) = LayerFilter.Value[At.type, T](at)

  /** Define At filter for a DateTime */
  implicit def forDateTime[K: TemporalComponent : Boundable, M] =
    new LayerFilter[K, At.type, DateTime, M] {
      def apply(metadata: M, kb: KeyBounds[K], at: DateTime) = {
        val queryBounds = KeyBounds(
          kb.minKey setComponent TemporalKey(at),
          kb.maxKey setComponent TemporalKey(at))
        (queryBounds intersect kb) match {
          case kb: KeyBounds[K] => List(kb)
          case EmptyBounds => Nil
        }
      }
    }
}

object Between {
  def apply[T](start: T, end: T) = LayerFilter.Value[Between.type, (T, T)](start -> end)

  /** Define Between filter for a tuple of DateTimes */
  implicit def forDateTimeTuple[K: TemporalComponent : Boundable, M] =
    new LayerFilter[K, Between.type, (DateTime, DateTime), M] {
      def apply(metadata: M, kb: KeyBounds[K], range: (DateTime, DateTime)) = {
        val queryBounds = KeyBounds(
          kb.minKey setComponent TemporalKey(range._1),
          kb.maxKey setComponent TemporalKey(range._2))
        (queryBounds intersect kb) match {
          case kb: KeyBounds[K] => List(kb)
          case EmptyBounds => Nil
        }
      }
    }
}

object Contains {
  def apply[T](value: T) = LayerFilter.Value[Contains.type, T](value)

  /** Define Intersects filter for Extent */
  implicit def forPoint[K: SpatialComponent: Boundable, M: (? => MapKeyTransform)] =
    new LayerFilter[K, Contains.type, Point, M] {
    def apply(metadata: M, kb: KeyBounds[K], point: Point) = {
      val spatialKey = (metadata: MapKeyTransform)(point)
      val queryBounds =
        KeyBounds(
          kb.minKey setComponent spatialKey,
          kb.maxKey setComponent spatialKey
        )
      (queryBounds intersect kb) match {
        case kb: KeyBounds[K] => List(kb)
        case EmptyBounds => Nil
      }
    }
  }
}
