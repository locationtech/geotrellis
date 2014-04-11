package geotrellis.feature.json

import spray.httpx.SprayJsonSupport
import spray.json._

/**
 * A trait providing automatic to and from JSON marshalling/unmarshalling using spray-json implicits.
 * parameter for writing json and will attempt to attach it to Feature/Geometry json representations.
 */
trait GeoJsonSupport extends SprayJsonSupport with GeometryFormats with FeatureFormats with CrsFormats with DefaultJsonProtocol

object GeoJsonSupport extends GeoJsonSupport