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

package geotrellis.vectortile

import geotrellis.vector._

/**
  * A case class that conforms to the Mapbox Vector Tile 2.0 specification
  *
  * Users are responsible for converting their geometries, features and other
  * objects to MVTFeatures so that they can be written to mvts using
  * [[VectorTile]].
  *
  * https://docs.mapbox.com/vector-tiles/specification/
  *
  * @param id
  * @param geom
  * @param data
  * @tparam G
  */
case class MVTFeature[+G <: Geometry](id: Option[Long], geom: G, data: Map[String, Value]) {}

object MVTFeature {
  def apply[G <: Geometry](geom: G, data: Map[String, Value]): MVTFeature[G] = MVTFeature(None, geom, data)
}
