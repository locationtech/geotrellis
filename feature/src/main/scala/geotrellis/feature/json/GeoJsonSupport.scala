package geotrellis.feature.json

import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol

/**
 * Can be mixed in with a spray service (or imported) to allow marshalling of Geometry and Feature objects
 */
trait GeoJsonSupport extends SprayJsonSupport with GeometryFormats with FeatureFormats with DefaultJsonProtocol
object GeoJsonSupport extends GeoJsonSupport