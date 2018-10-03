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

package geotrellis.vector
package prepared

import org.locationtech.jts.geom.prep.PreparedGeometryFactory

/** This case class provides functionality corresponding to JTS' prepared geometries.
  * Prepared geometries carry out some computations which, in certain cases, provide significant
  * performance enhancements to operations over those geometries. See Chris Bennight's
  * [[https://github.com/chrisbennight/intersection-test writeup and benchmarks]] for more.
  */
case class PreparedGeometry[G <: Geometry](val geom: Geometry) {
  private val prepared = PreparedGeometry.factory.create(geom.jtsGeom)
  def contains(that: Geometry): Boolean =
    prepared.contains(that.jtsGeom)

  def containsProperly(that: Geometry): Boolean =
    prepared.containsProperly(that.jtsGeom)

  def coveredBy(that: Geometry): Boolean =
    prepared.coveredBy(that.jtsGeom)

  def covers(that: Geometry): Boolean =
    prepared.covers(that.jtsGeom)

  def crosses(that: Geometry): Boolean =
    prepared.crosses(that.jtsGeom)

  def disjoint(that: Geometry): Boolean =
    prepared.disjoint(that.jtsGeom)

  def intersects(that: Geometry): Boolean =
    prepared.intersects(that.jtsGeom)

  def overlaps(that: Geometry): Boolean =
    prepared.overlaps(that.jtsGeom)

  def touches(that: Geometry): Boolean =
    prepared.touches(that.jtsGeom)

  def within(that: Geometry): Boolean =
    prepared.within(that.jtsGeom)
}

/** Companion object to [[PreparedGeometry]] */
object PreparedGeometry {
  private val factory = new PreparedGeometryFactory
}

