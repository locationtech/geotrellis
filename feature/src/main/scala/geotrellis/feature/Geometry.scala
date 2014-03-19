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

package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}

trait Geometry {

  val jtsGeom: jts.Geometry

  def distance(other: Geometry): Double =
    jtsGeom.distance(other.jtsGeom)

  def withinDistance(other: Geometry, dist: Double): Boolean =
    jtsGeom.isWithinDistance(other.jtsGeom, dist)

  def centroid: PointOrNoResult = 
    jtsGeom.getCentroid 

  def interiorPoint: PointOrNoResult = 
    jtsGeom.getInteriorPoint 

  // TODO: decide if this should be the Object.equals() override
  def equals(g: Geometry): Boolean =
    jtsGeom.equals(g)

}

trait Relatable { self: Geometry =>

  def intersects(other: Geometry): Boolean =
    jtsGeom.intersects(other.jtsGeom)

  def disjoint(other: Geometry): Boolean =
    jtsGeom.disjoint(other.jtsGeom)

}

trait MultiGeometry extends Geometry


  /* TO BE IMPLEMENTED ON A PER TYPE BASIS */

  // equal (with tolerance?)
  // equalExact (huh?)
  // normalize (hmmm)

  // isValid ( don't allow invalid? )

  // something with relate if it's fast (benchmark)

  /* IMPLEMENTED */

  // boundary
  // intersection ( & )
  // union ( | )
  // difference ( - )

  // crosses
  // within
  // contains - opposite of within

  // vertices - line, polygon; doesn't make much sense for a point
  // envelope - line, polygon; again, doesn't make sense for points since it just returns the point
  // boundingBox - same thing as envelope
  // length - line; points have length 0.0
  // perimeter - length of a polygon

  // isSimple - always true for valid polygons and empty geoms; true for points as well; false for MultiPoints with repeated points
  // overlaps - geoms must have same dimension and not all points in common and intersection of interiors has same dimension as geoms themselves - done for P/L/A


  // buffer - None on collections, always a polygon. (wait maybe on Multli's)
  // contains - Not on collections (wait maybe on Multli's) - if not, then other Geometry methods don't belong.
  // isRectangle (polygon)
  // def area:Double = geom.getArea  (not for points?)

  // TODO: handle Topology Exception from symDifference, union, intersection, difference
  // symDifference - can't have a GC as an arg. May throw a TopologyException - how to deal with this?

  // def boundary = jts.getBoundary
  // def boundaryDimension = jts.getBoundaryDimension
  // def centroid = jts.getCentroid
  // def coordinate:(Double,Double) = jts.getCoordinate
  // def coordinates:Seq[(Double,Double)] = jts.getCoordinates
  // def dimension = jts.getDimension
