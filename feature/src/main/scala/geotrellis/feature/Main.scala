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

package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}

abstract sealed class GeometryType

object GeometryType {

  implicit def jtsToGeometryType(geom: jts.Geometry) =
    stringToGeometryType(geom.getGeometryType)

  implicit def stringToGeometryType(str: String) =
    str match {
      case "GeometryCollection" => GeometryCollectionType
      case "Point" => PointType
      case "LineString" => LineStringType
      case "LinearRing" => LinearRingType
      case "Polygon" => PolygonType
      case "MultiPoint" => MultiPointType
      case "MultiLineString" => MultiLineStringType
      case "MultiPolygon" => MultiPolygonType

    }
}

case object GeometryCollectionType extends GeometryType
case object PointType extends GeometryType
case object LineStringType extends GeometryType
case object LinearRingType extends GeometryType
case object PolygonType extends GeometryType
case object MultiPointType extends GeometryType
case object MultiLineStringType extends GeometryType
case object MultiPolygonType extends GeometryType

object Main {
  def main(args:Array[String]) = {

  }
}

