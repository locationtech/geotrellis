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

package geotrellis.vector.voronoi

import geotrellis.vector._

import com.vividsolutions.jts.geom.{GeometryCollection, Coordinate, Point => JTSPoint, Polygon => JTSPolygon}

/**
 * A class to compute the Voronoi diagram of a collection of points.  The Voronoi
 * diagram partitions the plane into a set of non-overlapping convex polygonal
 * cells in one-to-one correspondence with the input points.  The cells are
 * defined as the region in the plane closer to the corresponding point than any
 * other point.
 *
 * This class is a simple wrapper around functionality provided by JTS.  That
 * package elected to compute the Voronoi cells bounded by an arbitrary bounding
 * triangle.  While this bounding triangle is large, it is not infinite nor
 * user-specifiable.  If the target point set inhabits a region that is small with
 * respect to the domain of interest, you may have to handle cell boundaries.
 */
class Voronoi(val verts: Array[Point]) {

  val del = Delaunay(verts)

  @deprecated("use VoronoiDiagram.voronoiCells() instead", "1.2")
  def voronoiCells(): Seq[Polygon] = {
    val vd = del.subd.getVoronoiDiagram(del.gf).asInstanceOf[GeometryCollection]
    for ( i <- 0 until vd.getNumGeometries) yield Polygon(vd.getGeometryN(i).asInstanceOf[JTSPolygon])
  }

  @deprecated("use VoronoiDiagram.voronoiCellsWithPoints() instead", "1.2")
  def voronoiCellsWithPoints: Seq[(Polygon, Point)] = {
    val vd = del.subd.getVoronoiDiagram(del.gf).asInstanceOf[GeometryCollection]
    for ( i <- 0 until vd.getNumGeometries) yield {
      val jtspoly = vd.getGeometryN(i).asInstanceOf[JTSPolygon]
      val jtscenter = jtspoly.getUserData.asInstanceOf[Coordinate]
      (Polygon(jtspoly), Point.jtsCoord2Point(jtscenter))
    }
  }
}

