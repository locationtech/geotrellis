package geotrellis.spark.io

import com.github.nscala_time.time.Imports._
import geotrellis.raster.{GridBounds, RasterExtent}
import geotrellis.spark._
import geotrellis.spark.tiling.MapKeyTransform
import geotrellis.vector.{Extent, Point, MultiPolygon}

import scala.annotation.implicitNotFound

import scala.collection.mutable

@implicitNotFound("Unable to filter ${K} by ${F} given ${M}, Please provide RDDFilter[${K}, ${F}, ${T}, ${M}]")
trait RDDFilter[K, F, T, M] {
  /** Should reduce one of the dimensions in KeyBounds using information from param
    *
    * @param metadata  M of the layer being filtered
    * @param kb        KeyBounds within the layer, possibly already reduce for max
    * @param param     Parameter to the filter, contains information to restrict kb
    */
  def apply(metadata: M, kb: KeyBounds[K], param: T): Seq[KeyBounds[K]]

  import RDDFilter._
  /** Applies all the filters contained in the expression tree to input KeyBounds.
    * Resulting list may be equal to or less than the number of [[Value]]s in ast. */
  def apply(metadata: M, kb: KeyBounds[K], ast: Expression[_, T])(implicit boundable: Boundable[K]): List[KeyBounds[K]] = {
    def flatten(metadata: M, kb: KeyBounds[K], ast: Expression[_, T]): Seq[KeyBounds[K]] =
      ast match {
        case Value(x) => apply(metadata, kb, x)
        case Or(v1, v2) => flatten(metadata, kb, v1) ++ flatten(metadata,kb, v2)
      }

    val keyBounds = flatten(metadata, kb, ast)
    keyBounds.combinations(2).foreach { case Seq(a, b) =>
      if (boundable.intersects(a, b))
        sys.error(s"Query expression produced intersecting bounds, only non-intersecting regions are supported. ($a, $b)")
    }

    keyBounds.toList
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
  import geotrellis.raster.rasterize.{Rasterizer, Callback}
  import scala.collection.mutable.ListBuffer

  def apply[T](value: T) = RDDFilter.Value[Intersects.type, T](value)

  /** Define Intersects filter for KeyBounds */
  implicit def forKeyBounds[K: Boundable, M] =
    new RDDFilter[K, Intersects.type, KeyBounds[K], M] {
      def apply(metadata: M, kb1: KeyBounds[K], kb2: KeyBounds[K]) = {
        implicitly[Boundable[K]].intersect(kb2, kb1).toSeq
      }
    }

  /** Define Intersects filter for GridBounds */
  implicit def forGridBounds[K: SpatialComponent: Boundable, M] =
    new RDDFilter[K, Intersects.type, GridBounds, M] {
      def apply(metadata: M, kb: KeyBounds[K], bounds: GridBounds) = {
        val queryBounds = KeyBounds(
          kb.minKey updateSpatialComponent SpatialKey(bounds.colMin, bounds.rowMin),
          kb.maxKey updateSpatialComponent SpatialKey(bounds.colMax, bounds.rowMax))
        implicitly[Boundable[K]].intersect(queryBounds, kb).toSeq
      }
    }

  /** Define Intersects filter for Extent */
  implicit def forExtent[K: SpatialComponent: Boundable, M: (? => {def mapTransform: MapKeyTransform})] =
    new RDDFilter[K, Intersects.type, Extent, M] {
    def apply(metadata: M, kb: KeyBounds[K], extent: Extent) = {
      val bounds = metadata.mapTransform(extent)
      val queryBounds = KeyBounds(
        kb.minKey updateSpatialComponent SpatialKey(bounds.colMin, bounds.rowMin),
        kb.maxKey updateSpatialComponent SpatialKey(bounds.colMax, bounds.rowMax))
      implicitly[Boundable[K]].intersect(queryBounds, kb).toSeq
    }
  }

  /** Define Intersects filter for Polygon */
  implicit def forPolygon[K: SpatialComponent: Boundable, M] =
    new RDDFilter[K, Intersects.type, MultiPolygon, M] {
      def apply(metadata: M, kb: KeyBounds[K], polygon: MultiPolygon) = {
        val extent = polygon.envelope
        val keyext = metadata.asInstanceOf[RasterMetaData].mapTransform(kb.minKey)
        val bounds = metadata.asInstanceOf[RasterMetaData].mapTransform(extent)

        /*
         * Construct a rasterExtent that fits tightly around the
         * candidate tiles (the candidate keys).  IT IS ASSUMED THAT
         * ALL TILES HAVE THE SAME LENGTH AND HEIGHT.
         */
        val xmin = math.floor(extent.min.x / keyext.width) * keyext.width
        val ymin = math.floor(extent.min.y / keyext.height) * keyext.height
        val xmax = math.ceil(extent.max.x / keyext.width) * keyext.width
        val ymax = math.ceil(extent.max.y / keyext.height) * keyext.height
        val rasterExtent = RasterExtent(Extent(xmin, ymin, xmax, ymax), bounds.width, bounds.height)

        /*
         * Use the Rasterizer to construct a list of tiles which meet
         * the query polygon.  That list of tiles is stored as an
         * array of tuples which is then mapped-over to produce an
         * array of KeyBounds (where the keys and KeyBounds are of the
         * correct type).
         */
        val tiles = new mutable.HashSet[(Int, Int)] with mutable.SynchronizedSet[(Int, Int)]

        Rasterizer.foreachCellByMultiPolygon(polygon, rasterExtent, true)( new Callback {
          def apply(col : Int, row : Int): Unit = {
            val tile : (Int, Int) = (bounds.colMin + col, bounds.rowMin + row)
            tiles += tile
          }
        })

        tiles
          .map({ tile =>
            val qb = KeyBounds(
              kb.minKey updateSpatialComponent SpatialKey(tile._1, tile._2),
              kb.maxKey updateSpatialComponent SpatialKey(tile._1, tile._2))
            implicitly[Boundable[K]].intersect(qb, kb).toSeq })
          .reduce({ (x,y) => x ++ y })
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
        implicitly[Boundable[K]].intersect(queryBounds, kb).toSeq
      }
    }
}

object Contains {
  def apply[T](value: T) = RDDFilter.Value[Contains.type, T](value)

  /** Define Intersects filter for Extent */
  implicit def forPoint[K: SpatialComponent: Boundable, M: (? => {def mapTransform: MapKeyTransform})] =
    new RDDFilter[K, Contains.type, Point, M] {
    def apply(metadata: M, kb: KeyBounds[K], point: Point) = {
      val spatialKey = metadata.mapTransform(point)
      val queryBounds =
        KeyBounds(
          kb.minKey updateSpatialComponent spatialKey,
          kb.maxKey updateSpatialComponent spatialKey
        )
      implicitly[Boundable[K]].intersect(queryBounds, kb).toSeq
    }
  }
}
