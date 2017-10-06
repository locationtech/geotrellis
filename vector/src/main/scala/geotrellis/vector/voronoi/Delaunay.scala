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

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory, MultiPoint, Polygon => JTSPolygon}
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder

import spire.syntax.cfor._


object Delaunay {
  @deprecated("use DelaunayTriangulation.apply() instead", "1.2")
  def apply(xs: Array[Double], ys: Array[Double]) =
    new Delaunay(xs.zip(ys).map({ case (x, y) => Point(x,y) }))

  @deprecated("use DelaunayTriangulation.apply() instead", "1.2")
  def apply(coords: Array[Coordinate]) =
    new Delaunay(coords.map({ coord => Point(coord.x, coord.y) }))

  @deprecated("use DelaunayTriangulation.apply() instead", "1.2")
  def apply(coords: Array[(Double, Double)]) =
    new Delaunay(coords.map({ case (x, y) => Point(x, y) }))

}

/**
  * A class for triangulating a set of points to satisfy the delaunay
  * property.  Each resulting triangle's circumscribing circle will
  * contain no other points of the input set.
  */
case class Delaunay(verts: Array[Point]) {

  private[voronoi] val gf = new GeometryFactory
  private val sites = new MultiPoint(verts.map(_.jtsGeom), gf)
  private val builder = new DelaunayTriangulationBuilder
  builder.setSites(sites)
  private[voronoi] val subd = builder.getSubdivision

  @deprecated("use DelaunayTriangulation instead", "1.2")
  val triangles: Seq[Polygon] = {
    val tris = subd.getTriangles(gf)
    val len = tris.getNumGeometries
    val arr = Array.ofDim[Polygon](len)
    cfor(0)(_ < len, _ + 1) { i => arr(i) = Polygon(tris.getGeometryN(i).asInstanceOf[JTSPolygon]) }
    arr
  }

}
