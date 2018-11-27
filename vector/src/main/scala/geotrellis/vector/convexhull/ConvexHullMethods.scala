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

package geotrellis.vector.convexhull

import geotrellis.util.MethodExtensions
import geotrellis.vector._

import org.locationtech.jts.{geom => jts}

/** Trait used to implicitly extend [[Geometry]] instances with a convex hull method */
trait ConvexHullMethods[G <: Geometry] extends MethodExtensions[G] {

  /** Generate the smallest possible, convex polygon which includes all points */
  def convexHull(): PolygonOrNoResult = {
    val geom = self.jtsGeom.convexHull
    geom match {
      case g: jts.Geometry if g.isEmpty => NoResult
      case p: jts.Polygon => PolygonResult(p)
      // Degenerate cases - 1 or 2 point geometries
      case l: jts.LineString => NoResult
      case p: jts.Point => NoResult
      case _ =>
        sys.error(s"Unexpected result: ${geom.getGeometryType}")
    }
  }
}
