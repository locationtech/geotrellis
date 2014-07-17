package geotrellis.feature.json

import spray.httpx.SprayJsonSupport
import spray.json._

/**
 * A trait providing automatic to and from JSON marshalling/unmarshalling using spray-json implicits.
 * parameter for writing json and will attempt to attach it to Feature/Geometry json representations.
 */
trait GeoJsonSupport extends GeometryFormats with FeatureFormats with CrsFormats

object GeoJsonSupport extends GeoJsonSupport
