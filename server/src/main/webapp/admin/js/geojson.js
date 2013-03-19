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
