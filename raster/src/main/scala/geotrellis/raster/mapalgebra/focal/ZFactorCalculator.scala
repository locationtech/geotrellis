package geotrellis.raster.mapalgebra.focal

import geotrellis.vector.{Extent, Point}

import org.apache.commons.math3.analysis.interpolation._


/** Produces a ZFactor for a given point using the prescribed
 *  conversion method.
 *
 *  @param produceZFactor A function that takes a latitude point and
 *    converts it to its corresponding ZFactor.
 */
class ZFactorCalculator(produceZFactor: Double => Double) extends Serializable {
  /** Produces the associated ZFactor for the given location.
   *
   *  @param extent The aread of interest. The center point will be used to determine
   *    the ZFactor.
   */
  def deriveZFactor(extent: Extent): Double =
    deriveZFactor(extent.center)

  /** Produces the associated ZFactor for the given point. */
  def deriveZFactor(point: Point): Double =
    deriveZFactor(point.y)

  def deriveZFactor(lat: Double): Double =
    produceZFactor(lat)
}

/** When creating a ZFactorCalculator, the projection of the target
 *  Tile(s) needs to be taken into account. If the Tiles are in
 *  LatLng, then the conversion between Latitude and ZFactor can
 *  already be calculated. Otherwise, one will need to supply the
 *  transformation required to produce the ZFactor.
 */
object ZFactorCalculator {
  final val LAT_LNG_FEET_AT_EQUATOR = 365217.6
  final val LAT_LNG_METERS_AT_EQUATOR = 11320

  /** Creates a [[ZFactorCalculator]] specifically for Tiles that are in
   *  LatLng.
   *
   *  @param unit The [[TileUnit]] type that the Tiles are in.
   */
  def createLatLngCalculator(unit: TileUnit): ZFactorCalculator =
    unit match {
      case Feet =>
        ZFactorCalculator((lat: Double) => 1 / (LAT_LNG_FEET_AT_EQUATOR * math.cos(math.toRadians(lat))))
      case Meters =>
        ZFactorCalculator((lat: Double) => 1 / (LAT_LNG_METERS_AT_EQUATOR * math.cos(math.toRadians(lat))))
    }

  /** Creates a [[ZFactorCalculator]] which uses linear interplation to calculate
   *  ZFactors. The linear interploation itself is derived from the values given.
   *
   *  @param mappedLatitudes A Map that maps latitudes to ZFactors. It is not required
   *    to supply a ZFactor for every latitude intersected by the Tile(s). Rather,
   *    based on the values given, a linear interpolation will be created and
   *    any latitude not mapped will have its associated ZFactor derived from
   *    that interpolation.
   */
  def createCalculator(mappedLatitudess: Map[Double, Double]): ZFactorCalculator = {
    val interp = new LinearInterpolator()
    val spline = interp.interpolate(mappedLatitudes.keys.toArray, mappedLatitudes.values.toArray)

    ZFactorCalculator((lat: Double) => spline.value(lat))
  }

  def apply(produceZFactor: Double => Double): ZFactorCalculator =
    new ZFactorCalculator(produceZFactor)
}
