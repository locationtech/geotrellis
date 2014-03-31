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

package geotrellis.io

import geotrellis._
import geotrellis.feature._

import geotrellis.data.geojson.GeoJsonReader

/**
 * Load a feature from GeoJson.
 *
 * This operation loads a feature from GeoJson.  It accepts both simple
 * geometry definitions and feature definitions.  If there is a property
 * JSON clause, the feature data will be Some(JsonNode).
 */
case class LoadGeoJsonFeature(geojson:Op[String]) extends Op1(geojson)({
  (geojson) => {
    val resultOpt = GeoJsonReader.parse(geojson)
    resultOpt match {
      case None => StepError("Could not parse GeoJSON", "")
      case Some(geometryArray) => {
        val geometryCount = geometryArray.length
        if (geometryCount != 1) {
          StepError("Expected a single feature; found %d features".format(geometryCount), "")
        } else {
          Result( geometryArray(0) )
        }
      }
    } 
  }
})
