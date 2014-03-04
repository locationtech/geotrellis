/*******************************************************************************
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
 ******************************************************************************/

var GJ = (function () {
    var fromPolygon = function(layer) {
        var latlngs = layer.getLatLngs();
        var a = []
        for(var i = 0; i < latlngs.length; i++ ) { 
            a.push([latlngs[i].lat,latlngs[i].lng]) 
        }

        return JSON.stringify({ 
            "type" : "Feature",
            "properties" : {},
            "geometry" : {
                "type" : "Polygon",
                "coordinates" : [a]
            }
        })
    }
    return {
        // Creates GeoJson from a leaflet Polygon.
        fromPolygon : fromPolygon
    }
})();
