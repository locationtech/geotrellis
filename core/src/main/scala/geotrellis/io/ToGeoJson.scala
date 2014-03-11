/***
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
 ***/

package geotrellis.io

import geotrellis._
import geotrellis.feature._
import geotrellis.logic.Collect

import geotrellis.data.geojson.GeoJsonWriter

object ToGeoJson {
  def apply[T](features:Op[Seq[Op[Geometry[T]]]])
              (implicit d:DummyImplicit):FeatureCollectionToGeoJson[T] =
    FeatureCollectionToGeoJson(Collect(features))

  def apply[T](features:Op[Seq[Geometry[T]]]):FeatureCollectionToGeoJson[T] =
    FeatureCollectionToGeoJson(features)
}

case class ToGeoJson[T](feature:Op[Geometry[T]]) extends Op1(feature)({
  feature =>
    Result(GeoJsonWriter.createString(feature))
})

case class FeatureCollectionToGeoJson[T](features:Op[Seq[Geometry[T]]]) 
extends Op1(features)({
  features =>
    Result(GeoJsonWriter.createFeatureCollectionString(features))
})
