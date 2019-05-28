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

package geotrellis.layers

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.{GridBounds, RasterExtent, PixelIsArea}
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.tiling._
import geotrellis.layers._
import geotrellis.vector._
import geotrellis.util._

import scala.annotation.implicitNotFound
import java.time.ZonedDateTime

@implicitNotFound("Unable to filter ${K} by ${F} given ${M}, Please provide LayerFilter[${K}, ${F}, ${T}, ${M}]")
trait LayerFilter[K, F, T, M] {
  /** Should reduce one of the dimensions in KeyBounds using information from param
    *
    * @param metadata  M of the layer being filtered
    * @param kb        KeyBounds within the layer, possibly already reduce for max
    * @param param     Parameter to the filter, contains information to restrict kb
    *
    * @note            The KeyBounds returned must be non-overlapping.
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

    flatten(metadata, kb, ast).toList
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
    new LayerFilter[K, Intersects.type, TileBounds, M] {
      def apply(metadata: M, kb: KeyBounds[K], bounds: TileBounds) = {
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

  /** Define Intersects filter for MultiPolygon */
  implicit def forMultiPolygon[K: SpatialComponent: Boundable, M: GetComponent[?, LayoutDefinition]] =
    new LayerFilter[K, Intersects.type, MultiPolygon, M] {
      def apply(metadata: M, kb: KeyBounds[K], polygon: MultiPolygon) = {
        val mapTransform = metadata.getComponent[LayoutDefinition].mapTransform
        mapTransform.multiPolygonToKeys(polygon)
          .map({ key =>
            val qb = KeyBounds(
              kb.minKey setComponent key,
              kb.maxKey setComponent key)
            qb intersect kb match {
              case kb: KeyBounds[K] => List(kb)
              case EmptyBounds => Nil
            }
          })
          .reduce({ (x,y) => x ++ y })
      }
    }

  /** Define Intersects filter for Polygon */
  implicit def forPolygon[K: SpatialComponent: Boundable, M: GetComponent[?, LayoutDefinition]] =
    new LayerFilter[K, Intersects.type, Polygon, M] {
      def apply(metadata: M, kb: KeyBounds[K], polygon: Polygon) =
        forMultiPolygon[K, M].apply(metadata, kb, MultiPolygon(polygon))
    }

  implicit def forProjectedPolygon[
    K: SpatialComponent: Boundable,
    M: GetComponent[?, LayoutDefinition]: GetComponent[?, CRS]
  ] =
    new LayerFilter[K, Intersects.type, (Polygon, CRS), M] {
      def apply(metadata: M, kb: KeyBounds[K], projectedPolygon: (Polygon, CRS)) = {
        val (polygon, srcCRS) = projectedPolygon
        val layerCRS = metadata.getComponent[CRS]

        if (srcCRS != layerCRS)
          forMultiPolygon[K, M].apply(metadata, kb, MultiPolygon(polygon.reproject(srcCRS, layerCRS)))
        else
          forMultiPolygon[K, M].apply(metadata, kb, MultiPolygon(polygon))
      }
    }

  implicit def forProjectedMultiPolygon[
    K: SpatialComponent: Boundable,
    M: GetComponent[?, LayoutDefinition]: GetComponent[?, CRS]
  ] =
    new LayerFilter[K, Intersects.type, (MultiPolygon, CRS), M] {
      def apply(metadata: M, kb: KeyBounds[K], projectedMultiPolygon: (MultiPolygon, CRS)) = {
        val (multiPolygon, srcCRS) = projectedMultiPolygon
        val layerCRS = metadata.getComponent[CRS]

        if (srcCRS != layerCRS)
          forMultiPolygon[K, M].apply(metadata, kb, multiPolygon.reproject(srcCRS, layerCRS))
        else
          forMultiPolygon[K, M].apply(metadata, kb, multiPolygon)
      }
    }

  /** Define Intersects filter for MultiLine */
  implicit def forMultiLine[K: SpatialComponent: Boundable, M: GetComponent[?, LayoutDefinition]] =
    new LayerFilter[K, Intersects.type, MultiLine, M] {
      def apply(metadata: M, kb: KeyBounds[K], multiLine: MultiLine) = {
        val mapTransform = metadata.getComponent[LayoutDefinition].mapTransform
        mapTransform.multiLineToKeys(multiLine)
          .map({ key =>
            val qb = KeyBounds(
              kb.minKey setComponent key,
              kb.maxKey setComponent key)
            qb intersect kb match {
              case kb: KeyBounds[K] => List(kb)
              case EmptyBounds => Nil
            }
          })
          .reduce({ (x,y) => x ++ y })
      }
    }

  /** Define Intersects filter for Polygon */
  implicit def forLine[K: SpatialComponent: Boundable, M: GetComponent[?, LayoutDefinition]] =
    new LayerFilter[K, Intersects.type, Line, M] {
      def apply(metadata: M, kb: KeyBounds[K], line: Line) =
        forMultiLine[K, M].apply(metadata, kb, MultiLine(line))
    }

  /** Define Contains filter for Point */
  implicit def forPoint[K: SpatialComponent: Boundable, M: (? => MapKeyTransform)] =
    new LayerFilter[K, Intersects.type, Point, M] {
      def apply(metadata: M, kb: KeyBounds[K], point: Point) = Contains.pointContainment(metadata, kb, point)
    }
}

object At {
  def apply[T](at: T) = LayerFilter.Value[At.type, T](at)

  /** Define At filter for a DateTime */
  implicit def forDateTime[K: TemporalComponent : Boundable, M] =
    new LayerFilter[K, At.type, ZonedDateTime, M] {
      def apply(metadata: M, kb: KeyBounds[K], at: ZonedDateTime) = {
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
    new LayerFilter[K, Between.type, (ZonedDateTime, ZonedDateTime), M] {
      def apply(metadata: M, kb: KeyBounds[K], range: (ZonedDateTime, ZonedDateTime)) = {
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

  private[layers] def pointContainment[K: SpatialComponent: Boundable, M: (? => MapKeyTransform)](metadata: M, kb: KeyBounds[K], point: Point) = {
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

  /** Define Intersects filter for Extent */
  implicit def forPoint[K: SpatialComponent: Boundable, M: (? => MapKeyTransform)] =
    new LayerFilter[K, Contains.type, Point, M] {
      def apply(metadata: M, kb: KeyBounds[K], point: Point) = pointContainment(metadata, kb, point)
    }
}
