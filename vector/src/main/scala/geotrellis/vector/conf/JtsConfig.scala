package geotrellis.vector.conf

import geotrellis.util.LazyLogging

import com.vividsolutions.jts.geom.PrecisionModel
import com.vividsolutions.jts.precision.GeometryPrecisionReducer

case class Simplification(scale: Double = 1e12) {
  // 12 digits is maximum to avoid [[TopologyException]], see https://web.archive.org/web/20160226031453/http://tsusiatsoftware.net/jts/jts-faq/jts-faq.html#D9
  lazy val simplifier: GeometryPrecisionReducer = new GeometryPrecisionReducer(new PrecisionModel(scale))
}
case class Precision(`type`: String = "floating")
case class JtsConfig(precision: Precision = Precision(), simplification: Simplification = Simplification()) extends LazyLogging {
  val precisionType: String = precision.`type`
  val precisionModel: PrecisionModel = precisionType match {
    case "floating" => new PrecisionModel()
    case "floating_single" => new PrecisionModel(PrecisionModel.FLOATING_SINGLE)
    case "fixed" => new PrecisionModel(simplification.scale)
    case _ => throw new IllegalArgumentException(s"""Unrecognized JTS precision model, ${precisionType}; expected "floating", "floating_single", or "fixed" """)
  }
  val simplifier: GeometryPrecisionReducer = simplification.simplifier
}

object JtsConfig {
  lazy val conf: JtsConfig = pureconfig.loadConfigOrThrow[JtsConfig]("geotrellis.jts")
  implicit def jtsConfigToClass(obj: JtsConfig.type): JtsConfig = conf
}
