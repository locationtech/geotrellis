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

import org.locationtech.jts.{ geom => jts }
import org.locationtech.jts.triangulate.ConformingDelaunayTriangulationBuilder

import spire.syntax.cfor._


object ConformingDelaunay {

  def apply(xs: Array[Double], ys: Array[Double], constraints: jts.GeometryCollection) = {
    new ConformingDelaunay(
      xs.zip(ys).map({ case (x, y) => Point(x,y) }),
      constraints
    )
  }

  def apply(coords: Array[jts.Coordinate], constraints: jts.GeometryCollection) = {
    new ConformingDelaunay(
      coords.map({ coord => Point(coord.x, coord.y) }),
      constraints
    )
  }

  def apply(coords: Array[(Double, Double)], constraints: jts.GeometryCollection) = {
    new ConformingDelaunay(
      coords.map({ case (x, y) => Point(x, y) }),
      constraints
    )
  }

}

/**
  * A class for triangulating a set of points to satisfy the Delaunay
  * property (subject to the given linear constraints).  Each
  * resulting triangle's circumscribing circle will contain no other
  * points of the input set.
  */
case class ConformingDelaunay(
  verts: Array[Point],
  constraints: jts.GeometryCollection
) {

  private[voronoi] val gf = new jts.GeometryFactory
  private val sites = new jts.MultiPoint(verts.map(_.jtsGeom), gf)
  private val builder = new ConformingDelaunayTriangulationBuilder
  builder.setSites(sites) ; builder.setConstraints(constraints)
  private[voronoi] val subd = builder.getSubdivision

  val triangles: Seq[Polygon] = {
    val tris = subd.getTriangles(gf)
    val len = tris.getNumGeometries
    val arr = Array.ofDim[Polygon](len)
    cfor(0)(_ < len, _ + 1) { i => arr(i) = Polygon(tris.getGeometryN(i).asInstanceOf[jts.Polygon]) }
    arr
  }

  val steinerPoints: Seq[Point] = {
    val allPoints: Set[Point] = triangles.flatMap({ triangle => triangle.vertices }).toSet
    val givenPoints: Set[Point] = verts.toSet
    (allPoints &~ givenPoints).toList
  }

}
