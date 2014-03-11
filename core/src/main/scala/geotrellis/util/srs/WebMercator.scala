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

package geotrellis.util.srs

/**
 * WGS 84 Web Mercator, Spherical Mercator, EPSG:900913, EPSG:3857
 * 
 * http://spatialreference.org/ref/sr-org/7483/
 */
case object WebMercator extends SpatialReferenceSystem {
  val name = "Spherical Mercator EPSG:900913"

  def transform(x:Double,y:Double,targetSRS:SpatialReferenceSystem) =
    targetSRS match {
      case LatLng =>
        val xlng = (x / SpatialReferenceSystem.originShift) * 180.0
        val ylat1 = (y / SpatialReferenceSystem.originShift) * 180.0
        
        val ylat = 180 / math.Pi * (2 * math.atan( math.exp( ylat1 * math.Pi / 180.0)) - math.Pi / 2.0)
        (xlng, ylat)
      case _ =>
        throw new NoTransformationException(this,targetSRS)
    }
}
