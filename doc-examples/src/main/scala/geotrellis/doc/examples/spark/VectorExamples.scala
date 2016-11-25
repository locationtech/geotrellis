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

package geotrellis.doc.examples.vector

object VectorExamples {
  def `Writing a sequence of vector data to a GeoJson feature collection`: Unit = {
    import geotrellis.vector._
    import geotrellis.vector.io._

    // This import is important: otherwise the JsonFormat for the
    // feature data type is not available (the feature data type being Int)
    import spray.json.DefaultJsonProtocol._

    // Starting with a list of polygon features,
    // e.g. the return type of tile.toVector
    val features: List[PolygonFeature[Int]] = ???

    // Because we've imported geotrellis.vector.io, we get
    // GeoJson methods implicitly added to vector types,
    // including any Traversable[Feature[G, D]]

    val geojson: String = features.toGeoJson

    println(geojson)
  }
}
