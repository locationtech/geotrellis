package geotrellis.vector.io.json

import spray.json._

import geotrellis.vector._
import scala.util.{Try, Success, Failure}
import FeatureFormats._
import DefaultJsonProtocol._

/**
 * Accumulates GeoJson from Feature class instances and implements a Map keyed on geojson feature IDs.
 *
 * During serialization:
 * Each individual feature is parametrized on a class we need to accumulate geoJson per
 * instance of an object in order to use implicit scope resolution in finding the correct format.
 *
 * Features may be added using the .add, addAll methods, they are buffered as JsValues until .toJson is called
 *
 * During deserialization:
 * This object is instantiated with list of JsValues representing features.
 * It may be queried using .getAll[F <: Feature[_] ] method.
 *
 * It aggregates feature objects with data member still encoded in json
 */
class JsonFeatureCollectionMap(features: List[JsValue] = Nil) {
  private var buffer = features

  //-- Used for Serialization
  def add[D: JsonWriter](featureMap: (String, Feature[D])) =
    buffer = writeFeatureJsonWithID[D](featureMap) :: buffer
  def +=[D: JsonWriter](featureMap: (String, Feature[D])) = add(featureMap)

  def addAll[D: JsonWriter](featureMaps: Seq[(String, Feature[D])]) =
    featureMaps.foreach{ f => buffer = writeFeatureJsonWithID[D](f) :: buffer }
  def ++=[D: JsonWriter](featureMaps: Seq[(String, Feature[D])]) = addAll(featureMaps)

  def add(geometry: Geometry) =
    buffer = geometry.toJson :: buffer
  def +=(id: String, geometry: Geometry) = add(geometry)

  def addAll(geometries: Seq[Geometry]) =
    geometries.foreach{ f => add(f) }
  def ++=(geometries: Seq[Geometry]) = addAll(geometries)

  def toJson: JsValue =
    JsObject(
      "type" -> JsString("FeatureCollection"),
      "features" -> JsArray(buffer)
    )

  // This helper function is called below to grab the ID field for Map keys
  private def getFeatureID(js: JsValue): String = {
    js.asJsObject.getFields("id") match {
      case Seq(JsString(id)) => id
      case Seq(JsNumber(id)) => id.toString
      case _ => throw new DeserializationException("Feature expected to have \"ID\" field")
    }
  }


  //-- Used for Deserialization
  /**
   * This method locates the correct JsonFormat for F through implicit scope and
   * attempts to use it to parse each contained JsValue.
   *
   * @tparam F type of Feature to return
   * @return Vector or Feature objects that were successfully parsed
   */
  def getAll[F :JsonFormat]: Map[String, F] = {
    var ret = Map[String, F]()
    features.foreach{ f =>
      Try(f.convertTo[F]) match {
        case Success(feature) =>
          ret += getFeatureID(f) -> feature
        case _ => //didn't match, live to fight another match
      }
    }
    ret.toMap
  }

  def getAllFeatures[F <: Feature[_] :JsonFormat]: Map[String, F] =
    getAll[F]

  def getAllPointFeatures[D: JsonFormat]()         = getAll[PointFeature[D]]
  def getAllLineFeatures[D: JsonFormat]()          = getAll[LineFeature[D]]
  def getAllPolygonFeatures[D: JsonFormat]()       = getAll[PolygonFeature[D]]
  def getAllMultiPointFeatures[D: JsonFormat]()    = getAll[MultiPointFeature[D]]
  def getAllMultiLineFeatures[D: JsonFormat]()     = getAll[MultiLineFeature[D]]
  def getAllMultiPolygonFeatures[D: JsonFormat]()  = getAll[MultiPolygonFeature[D]]

  def getAllPoints()         = getAll[Point]
  def getAllLines()          = getAll[Line]
  def getAllPolygons()       = getAll[Polygon]
  def getAllMultiPoints()    = getAll[MultiPoint]
  def getAllMultiLines()     = getAll[MultiLine]
  def getAllMultiPolygons()  = getAll[MultiPolygon]

}

object JsonFeatureCollectionMap {
  def apply() = new JsonFeatureCollectionMap()

  def apply[D: JsonWriter](features: Traversable[(String, Feature[D])]) = {
    val fc = new JsonFeatureCollectionMap()
    fc ++= features.toList
    fc
  }

  def apply(geometries: Traversable[Geometry]) = {
    val fc = new JsonFeatureCollectionMap()
    fc ++= geometries.toList
    fc
  }
}
