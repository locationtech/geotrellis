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

package geotrellis.slick

import geotrellis.proj4.CRS
import geotrellis.vector._
import geotrellis.vector.reproject._


object Projected {
  implicit def toGeometry[G <: Geometry](p: Projected[G]) = p.geom
}

/**
 * This tuples Geometry with an SRID. It is up to the application developer to 
 * ensure that the SRID parameter stays semantically consistent.
 * 
 * Concretely this class exists because PostGIS requires an SRID to be stored 
 * with the Geometry and the decision has been made not to encapsulate SRID 
 * semantics in the Geometry hierarchy for the moment.
 *
 * Sample Usage: <code>
 * import geotrellis.proj4._
 * 
 * val projected = Point(1,1).withSRID(4326)  // LatLng, trust me
 * val projected = projected.reproject(LatLng, WebMercator)(3857) 
 * </code>
 */
case class Projected[+G <: Geometry](geom: G, srid: Int) {
  def reproject(src: CRS, dest: CRS)(destSRID: Int): Projected[G] =    
    Projected(Reproject(geom, src, dest).asInstanceOf[G], destSRID)

  def withSRID(newSRID: Int): Projected[G] = 
    Projected(geom, newSRID)
}
