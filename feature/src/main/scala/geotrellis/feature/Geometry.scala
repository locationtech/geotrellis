/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}

trait Geometry {

  val geom: jts.Geometry

  def centroid: Point = Point(geom.getCentroid)

  def interiorPoint: Point = Point(geom.getInteriorPoint)

  def coveredBy(other: Geometry): Boolean =
    geom.coveredBy(other.geom)

  def covers(other: Geometry): Boolean =
    geom.covers(other.geom)

  def intersects(other: Geometry): Boolean =
    geom.intersects(other.geom)

  def disjoint(other: Geometry): Boolean =
    geom.disjoint(other.geom)

  def touches(other: Geometry): Boolean =
    geom.touches(other.geom)

  def distance(other: Geometry) =
    geom.distance(other.geom)

  // Curious to benchmark this against .distance < d,
  // JTS implements it as a different op, I'm assuming
  // for speed.
  def withinDistance(other: Geometry, dist: Double) =
    geom.isWithinDistance(other.geom, dist)

  /* TO BE IMPLEMENTED ON A PER TYPE BASIS */

  // equal (with tolerance?)
  // equalExact (huh?)

  // vertices
  // envelope
  // boundingBox
  // length
  // perimeter

  // isSimple
  // isValid ( don't allow invalid? )
  // normalize (hmmm)
  // overlaps (needs interior to be same dimension as geoms, geom dims ==)
  // symDifference


  // something with relate if it's fast (benchmark)

  /* IMPLEMENTED */

  // boundary
  // intersection ( & )
  // union ( | )
  // difference ( - )

  // crosses
  // within


  // buffer - None on collections, always a polygon. (wait maybe on Multli's)
  // contains - Not on collections (wait maybe on Multli's) - if not, then other Geometry methods don't belong.
  // isRectangle (polygon)
  // def area:Double = geom.getArea  (not for points?)
  

  // def boundary = jts.getBoundary
  // def boundaryDimension = jts.getBoundaryDimension
  // def centroid = jts.getCentroid
  // def coordinate:(Double,Double) = jts.getCoordinate
  // def coordinates:Seq[(Double,Double)] = jts.getCoordinates
  // def dimension = jts.getDimension
}
