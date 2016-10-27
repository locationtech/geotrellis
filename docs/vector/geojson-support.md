# Parsing GeoTrellis objects from GeoJson #

GeoTrellis includes good support for serializing and deserializing geometry to/from GeoJson within the `geotrellis.vector.io.json` package.  Utilizing these features requires some instruction, however, since the interface may not be immediately apparent from the type signatures.

## Serializing to GeoJson ##

All `Geometry` and `Feature` objects in `geotrellis.vector` have a method extension providing a `toGeoJson` method.  This means that
```scala
import geotrellis.vector.io._
Polygon((10.0, 10.0), (10.0, 20.0), (30.0, 30.0), (10.0, 10.0)).toGeoJson
```
is valid (in this case yielding `{"type":"Polygon","coordinates":[[[10.0,10.0],[10.0,20.0],[30.0,30.0],[10.0,10.0]]]}`).

Issuing `.toGeoJson` on `Feature` instances, requires that the type parameter supplied to the feature meets certain requirements.  For example, `PointFeature(Point(0,0), 3)` will succeed, but to tag a Feature with arbitrary data, that data must be encapsulated in a case class.  That class must also be registered with the Json reading infrastructure provided by `spray`.  The following example achieves these goals:
```scala
import geotrellis.vector.io.json._

case class UserData(data: Int)
implicit val boxedValue = jsonFormat1(UserData)

PointFeature(Point(0,0), UserData(13))
```

Case classes with more than one argument would require the variants of `jsonFormat1` for classes of higher arity.  The output of the above snippet is
```{"type":"Feature","geometry":{"type":"Point","coordinates":[0.0,0.0]},"properties":{"data":13}}```
where the property has a single field named `data`.  Upon deserialization, it will be necessary for the data member of the feature to have fields with names that are compatible with the members of the feature's data type.

This is all necessary underpinning, but note that it is generally desirable to (de)serialize collections of features.  The serialization can be achieved by calling `.toGeoJson` on a `Seq[Feature[G, T]]`.  The result is a Json representation of a FeatureCollection.

## Deserializing from GeoJson ##

The return trip from a string representation can be accomplished by another method extension provided for strings: `parseGeoJson[T]`.  The only requirement for using this method is that the type of `T` must match the contents of the Json string.  If the Json string represents some `Geometry` subclass (i.e., `Point`, `MultiPolygon`, etc), then that type should be supplied to `parseGeoJson`.  This will work to make the return trip from any of the Json strings produced above.

Again, it is generally more interesting to consider Json strings that contain `FeatureCollection` structures.  These require more complex code.  Consider the following Json string:
```Scala
val fc: String = """{
                   |  "type": "FeatureCollection",
                   |  "features": [
                   |    {
                   |      "type": "Feature",
                   |      "geometry": { "type": "Point", "coordinates": [1.0, 2.0] },
                   |      "properties": { "someProp": 14 },
                   |      "id": "target_12a53e"
                   |    }, {
                   |      "type": "Feature",
                   |      "geometry": { "type": "Point", "coordinates": [2.0, 7.0] },
                   |      "properties": { "someProp": 5 },
                   |      "id": "target_32a63e"
                   |    }
                   |  ]
                   |}""".stripMargin
```
Decoding this structure will require the use of either `JsonFeatureCollection` or `JsonFeatureCollectionMap`; the former will return queries as a `Seq[Feature[G, T]]`, while the latter will return a `Map[String, Feature[G, T]]` where the key is the `id` field of each feature.  After calling
```scala
val collection = fc.parseGeoJson[JsonFeatureCollectionMap]
```
it will be necessary to extract the desired features from `collection`.  In order to maintain type safety, these results are pulled using accessors such as `.getAllPoints`, `.getAllMultiLineFeatures`, and so on.  Each geometry and feature type requires the use of a different method call.

As in the case of serialization, to extract the feature data from this example string, we must create a case class with an integer member named `someProp` and register it using `jsonFormat1`.
```scala
case class SomeProp(someProp: Int)
implicit val boxedToRead = jsonFormat1(SomeProp)

collection.getAllPointFeatures[SomeProp]
```

## A Note on Creating JsonFeatureCollectionMaps ##

It is straightforward to create FeatureCollection representations, as illustrated above.  Simply package your features into a `Seq` and call `toGeoJson`.  In order to name those features, however, it requires that a JsonFeatureCollectionMap be explicitly created.  For instance:
```scala
val fcMap = JsonFeatureCollectionMap(Seq("bob" -> Feature(Point(0,0), UserData(13))))
```
Unfortunately, the `toGeoJson` method is not extended to `JsonFeatureCollectionMap`, so we are forced to call `fcMap.toJson.toString` to get the same functionality.  The return of that call is
```json
{
  "type": "FeatureCollection",
  "features": [{
    "type": "Feature",
    "geometry": {
      "type": "Point",
      "coordinates": [0.0, 0.0]
    },
    "properties": {
      "data": 13
    },
    "id": "bob"
  }]
}
```
