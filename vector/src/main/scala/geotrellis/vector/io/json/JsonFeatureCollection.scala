package geotrellis.vector.json

import spray.json._

import geotrellis.vector._
import scala.util.{Try, Success, Failure}
import scala.collection.immutable.VectorBuilder
import FeatureFormats._
import DefaultJsonProtocol._

/**
 * Accumulates GeoJson from Feature class instances.
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
class JsonFeatureCollection(features: List[JsValue] = Nil) {
  private var buffer = features

  //-- Used for Serialization
  def add[D: JsonWriter](feature: Feature[D]) =
    buffer = writeFeatureJson[D](feature) :: buffer
  def +=[D: JsonWriter](feature: Feature[D]) = add(feature)

  def addAll[D: JsonWriter](features: Seq[Feature[D]]) =
    features.foreach{ f => buffer = writeFeatureJson[D](f) :: buffer }

  def ++=[D: JsonWriter](features: Seq[Feature[D]]) = addAll(features)

  def add(geometry: Geometry) =
    buffer = geometry.toJson :: buffer
  def +=(geometry: Geometry) = add(geometry)

  def addAll(geometries: Seq[Geometry]) =
    geometries.foreach(add(_))

  def ++=(geometries: Seq[Geometry]) = addAll(geometries)

  def toJson: JsValue =
    JsObject(
      "type" -> JsString("FeatureCollection"),
      "features" -> JsArray(buffer)
    )

  //-- Used for Deserialization
  /**
   * This method locates the correct JsonFormat for F through implicit scope and
   * attempts to use it to parse each contained JsValue.
   *
   * @tparam F type of Feature to return
   * @return Vector or Feature objects that were successfully parsed
   */
  def getAll[F :JsonFormat]: Vector[F] = {
    val ret = new VectorBuilder[F]()
    features.foreach{ f =>
      Try(f.convertTo[F]) match {
        case Success(feature) =>
          ret += feature
        case _ => //didn't match, live to fight another match
      }
    }
    ret.result()
  }

  def getAllFeatures[F <: Feature[_] :JsonFormat]: Vector[F] = 
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

object JsonFeatureCollection{
  def apply() = new JsonFeatureCollection()

  def apply[D: JsonWriter](features: Traversable[Feature[D]]) = {
    val fc = new JsonFeatureCollection()
    fc ++= features.toList
    fc
  }

  def apply(geometries: Traversable[Geometry]) = {
    val fc = new JsonFeatureCollection()
    fc ++= geometries.toList
    fc
  }
}
