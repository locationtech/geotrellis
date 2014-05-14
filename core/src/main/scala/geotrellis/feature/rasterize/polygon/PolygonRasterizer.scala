/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.feature.rasterize.polygon

import geotrellis._
import geotrellis.feature._
import geotrellis.feature.rasterize._

import scalaxy.loops._

object PolygonRasterizer {
  /**
   * Apply a function to each raster cell that intersects with a polygon.
   */
  def foreachCellByPolygon(p:Polygon, re:RasterExtent, includeExterior:Boolean=false)(f:Callback): Unit = 
    if (p.intersects(re.extent.asPolygon)) { 

      val (edges, rowMinOrg, rowMaxOrg) = {
        val TestLineSet(lines, rowMin, rowMax) =
          if(p.hasHoles) {
            (p.exterior :: p.holes.toList).foldLeft(TestLineSet.EMPTY) { (acc, l) =>
              acc.merge(TestLineSet(l, re))
            }
          } else {
            TestLineSet(p.exterior, re)
          }
        (lines.groupBy(_.rowMin), rowMin, rowMax)
       }

      var activeEdges:List[Intercept] = List[Intercept]()

      if (rowMaxOrg > 0 && rowMinOrg < re.rows) {
        val rowMin = math.max(0, rowMinOrg)
        val rowMax = math.min(re.rows - 1, rowMaxOrg)

        // Process each row in the raster that intersects with the polygon.
        for(row <- rowMinOrg to rowMaxOrg) {
          val y = re.gridRowToMap(row)

          // * Update the x intercepts of each line for the next line.
          activeEdges = activeEdges.map { edge => Intercept(edge.line, y, re) }

          val newEdges = 
            edges
              .getOrElse(row, List[TestLine]())
              .map( line => Intercept(line, y, re) )

          activeEdges = (activeEdges ++ newEdges).sortWith { (p1, p2) =>  p1.x < p2.x }

          // call function on included cells
          if (row >= rowMin && row <= rowMax) {
            for(List(leftLine, rightLine) <- activeEdges.grouped(2)) {
              val (minCol, maxCol) =
                if (includeExterior) {
                  ( math.floor(leftLine.colDouble).toInt, math.ceil(rightLine.colDouble).toInt )
                } else {
                  ( math.floor(leftLine.colDouble + 0.5).toInt,  math.floor(rightLine.colDouble - 0.5).toInt )
                }
              for(col <- math.max(minCol,0) to math.min(maxCol, re.cols - 1 )) {
                f(col, row)
              }
            }             
          }

          // ** Remove from AET those entries for which row = rowMax
          //     (Important to handle intercepts between two lines that are monotonically increasing/decreasing, as well
          //      y maxima, as well simply to drop edges that are no longer relevant)
          activeEdges = activeEdges.filter(_.line.rowMax != row)
        }
      }
    }
}
