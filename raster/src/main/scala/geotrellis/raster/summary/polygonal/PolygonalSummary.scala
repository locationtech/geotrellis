/*
 * Copyright 2019 Azavea
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

package geotrellis.raster.summary.polygonal

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.raster.summary.GridVisitor
import geotrellis.util.{GetComponent, MethodExtensions}
import spire.syntax.cfor._

object PolygonalSummary {
  final val DefaultOptions =
    Rasterizer.Options(includePartial = true, sampleType = PixelIsArea)

  /**
    * Perform a polygonal summary operation over a raster within the bounds of geometry.
    *
    * GridVisitor.visit is called for each matching pixel in the raster and rasterized geometry.
    *
    * @tparam A type of the Raster to perform the polygonal summary on
    * @tparam R type of the objects being summarized
    *
    * @return The result wrapped in [[PolygonalSummaryResult]] or NoIntersection
    */
  def apply[A <: Grid[Int], R](
                                raster: A,
                                geometry: Geometry,
                                gridVisitor: GridVisitor[A, R],
                                options: Rasterizer.Options
  )(implicit
    getRasterExtent: GetComponent[A, RasterExtent]): PolygonalSummaryResult[R] = {
    val rasterExtent: RasterExtent = getRasterExtent.get(raster)
    val rasterArea: Polygon = rasterExtent.extent.toPolygon
    if (rasterArea.disjoint(geometry)) {
      NoIntersection
    } else {
      geometry match {
        case area: TwoDimensions if (rasterArea.coveredBy(area)) =>
          cfor(0)(_ < rasterExtent.cols, _ + 1) { col =>
            cfor(0)(_ < rasterExtent.rows, _ + 1) { row =>
              gridVisitor.visit(raster, col, row)
            }
          }
        case _ =>
          Rasterizer.foreachCellByGeometry(geometry, rasterExtent, options) {
            (col: Int, row: Int) =>
              gridVisitor.visit(raster, col, row)
          }
      }
      Summary(gridVisitor.result)
    }
  }

  trait PolygonalSummaryMethods[A <: Grid[Int]] extends MethodExtensions[A] {

    def polygonalSummary[R](
                             geometry: Geometry,
                             gridVisitor: GridVisitor[A, R],
                             options: Rasterizer.Options
    )(implicit ev1: GetComponent[A, RasterExtent]): PolygonalSummaryResult[R] =
      PolygonalSummary(self, geometry, gridVisitor, options)

    def polygonalSummary[R](
        geometry: Geometry,
        gridVisitor: GridVisitor[A, R]
    )(implicit ev1: GetComponent[A, RasterExtent]): PolygonalSummaryResult[R] =
      PolygonalSummary(self,
                       geometry,
                       gridVisitor,
                       PolygonalSummary.DefaultOptions)
  }

  trait ToPolygonalSummaryMethods {
    implicit class withPolygonalSummaryMethods[A <: Grid[Int]](val self: A)
        extends PolygonalSummaryMethods[A]
  }

  object ops extends ToPolygonalSummaryMethods
}
