/*
 * Copyright 2020 Azavea
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

/**
 * An object containing duplicate geometry objects for construction of geometries
 * in the REPL environment.
 *
 * This object exists to work around a REPL bug which masks objects when instances
 * of classes with the same name are created.  DO NOT USE IN COMPILED CODE.  Use bare
 * objects instead (Point, LineString, etc).
 */
object JTS {
  object Point extends PointConstructors
  object LineString extends LineStringConstructors
  object Polygon extends PolygonConstructors
  object MultiPoint extends MultiPointConstructors
  object MultiLineString extends MultiLineStringConstructors
  object MultiPolygon extends MultiPolygonConstructors
  object GeometryCollection extends GeometryCollectionConstructors
}
