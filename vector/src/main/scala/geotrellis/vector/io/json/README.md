#Parsing geojson in geotrellis.vector.json


Say we’re looking to go from a string to a feature collection object as represented within the geotrellis library. The task involves a number of steps and requires keeping track of a number of implicit values.

Ultimately, we want to move from a geojson string:
```Scala
val x: String = “””{
                  |  “type”str: “FeatureCollection”,
                  |  “features”: [
                  |    {
                  |      “type”: “feature",
                  |      “geometry”: { “type”: “Point”, "coordinates”: [1.0, 2.0] },
                  |      “properties”: { “SomeProp": 14 }
                  |      “id”: “target_12a53e"
                  |    }, {
                  |      “type”: “feature",
                  |      “geometry”: { “type”: “Point”, "coordinates”: [2.0, 7.0] },
                  |      “properties”: { “SomeProp": 5 }
                  |      “id”: “target_32a63e"
                  |    }
                  |  ]
                  |}”””.stripMargin
```
To a Scala representation. For example, `JsonFeatureCollectionMap.getAllFeatures[PointFeature[SomeProp]]` should evaluate to:
```Scala
Map(
  ("target_12a53e" -> PointFeature(Point ( 1.0 2.0 ), SomeProp( 14 ))),
  ("target_12a53e" -> PointFeature(Point ( 2.0, 7.0 ), SomeProp( 5 )))
)
```

1. First, we've got to parse a string as a bit of json. For this (`parseGeoJson`) is found on `geotrellis.vector.json.package`. `parseGeoJson` takes one type parameter. We'll use `geotrellis.vector.json.JsonFeatureCollectionMap` in this case, since our geojson has IDs which we'll want to keep. This class represents IDs as keys to corresponding features.
```Scala
val jsonFeatureMapping = jsonString.parseGeoJson[geotrellis.vector.json.JsonFeatureCollectionMap]
```

2. Once we've parsed our string (which must be valid geojson!), it is time to query our JsonFeatureCollection for its points:
```Scala
jsonFeatureMapping.getAllFeatures[PointFeature[SomeProp]]
```
Note that we had to use a feature type parameter and a type parameter on the PointFeature. This is necessary so that the properties on our feature can be properly encoded as Scala objects. See geotrellis.vector.json.FeatureFormats for the implicits which use said type parameterization.
