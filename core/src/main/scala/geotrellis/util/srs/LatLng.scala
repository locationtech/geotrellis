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

package geotrellis.util.srs

/**
 * WGS 84 Datum ESPG:4326
 * 
 * http://spatialreference.org/ref/epsg/4326/
 */
case object LatLng extends SpatialReferenceSystem {
  val name = "WGS84 Datum EPSG:4326"

  def transform(x:Double,y:Double,targetSRS:SpatialReferenceSystem) =
    targetSRS match {
      case WebMercator =>
        val mx = x * SpatialReferenceSystem.originShift / 180.0
        val my1 = ( math.log( math.tan((90 + y) * math.Pi / 360.0 )) / (math.Pi / 180.0) )
        val my = my1 * SpatialReferenceSystem.originShift / 180 
        (mx, my)
      case _ =>
        throw new NoTransformationException(this,targetSRS)
    }
}
