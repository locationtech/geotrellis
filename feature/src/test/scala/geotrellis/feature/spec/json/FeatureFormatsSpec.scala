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

package geotrellis.feature.spec.json

import geotrellis.feature._
import geotrellis.feature.json._

import org.scalatest._
import spray.json._
import FeatureFormats._
import DefaultJsonProtocol._

class FeatureFormatsSpec extends FlatSpec with ShouldMatchers {
  "PointJsonFormat" should "be able to read a point json" in {
    val point = Point(100, 0)
    val json = """{"type":"Point","coordinates":[100.0,0.0]}"""

    json.asJson.convertTo[Point] should equal (point)
    point.toJson.compactPrint should equal (json)
    println(point.toJsonWith("TOP SECRET"))
  }

  "FeatureFormat" should "be fucking awesome" in {
    val point = Point(1,2)
    val pf = new PointFeature(point, "Payload")
    val json = """{"type":"Feature","geometry":{"type":"Point","coordinates":[1.0,2.0]},"properties":"Payload"}"""

    json.asJson.convertTo[PointFeature[String]] should equal (pf)
    pf.toJson.compactPrint should equal (json)
  }

}