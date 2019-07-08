package geotrellis.raster.mapalgebra.focal

import geotrellis.vector.{Extent, Point}

import org.apache.commons.math3.analysis.interpolation._


class ZFactorCalculator(produceZFactor: Double => Double) extends Serializable {
  def deriveZFactor(extent: Extent): Double =
    deriveZFactor(extent.center)

  def deriveZFactor(point: Point): Double =
    deriveZFactor(point.y)

  def deriveZFactor(lat: Double): Double =
    produceZFactor(lat)
}

object ZFactorCalculator {
  final val LAT_LNG_FEET_AT_EQUATOR = 365217.6
  final val LAT_LNG_METERS_AT_EQUATOR = 11320

  def createLatLngZFactorCalculator(unit: Unit): ZFactorCalculator =
    unit match {
      case Feet =>
        ZFactorCalculator((lat: Double) => 1 / (LAT_LNG_FEET_AT_EQUATOR * math.cos(math.toRadians(lat))))
      case Meters =>
        ZFactorCalculator((lat: Double) => 1 / (LAT_LNG_METERS_AT_EQUATOR * math.cos(math.toRadians(lat))))
    }

  def createZFactorCalculator(mappedLats: Map[Double, Double]): ZFactorCalculator = {
    val interp = new LinearInterpolator()
    val spline = interp.interpolate(mappedLats.keys.toArray, mappedLats.values.toArray)

    ZFactorCalculator((lat: Double) => spline.value(lat))
  }

  def apply(produceZFactor: Double => Double): ZFactorCalculator =
    new ZFactorCalculator(produceZFactor)
}
